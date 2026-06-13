/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 * 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.wtp.core.server.publish;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.jboss.ide.eclipse.archives.core.util.TrueZipUtil;
import org.jboss.ide.eclipse.as.core.server.IModulePathFilter;
import org.jboss.ide.eclipse.as.core.server.IModulePathFilterProvider;
import org.jboss.ide.eclipse.as.core.util.FileUtil;
import org.jboss.ide.eclipse.as.core.util.IEventCodes;
import org.jboss.ide.eclipse.as.core.util.ModuleResourceUtil;
import org.jboss.ide.eclipse.as.core.util.ProgressMonitorUtil;
import org.jboss.ide.eclipse.as.wtp.core.ASWTPToolsPlugin;
import org.jboss.ide.eclipse.as.wtp.core.Messages;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.util.PublishControllerUtil;
import org.jboss.ide.eclipse.as.wtp.core.util.ServerModelUtilities;

import de.schlichtherle.truezip.file.TFile;


public class LocalZippedModulePublishRunner extends ModuleResourceUtil {
	
	private IServer server;
	private IModule[] module;
	private IPath destinationArchive;
	private IModulePathFilterProvider filterProvider;
	public LocalZippedModulePublishRunner(IServer server, IModule module, IPath destinationArchive, IModulePathFilterProvider filterProvider) {
		this(server, new IModule[]{module}, destinationArchive, filterProvider);
	}
	
	public LocalZippedModulePublishRunner(IServer server, IModule[] module, IPath destinationArchive, IModulePathFilterProvider filterProvider) {
		this.server = server;
		this.module = module;
		this.destinationArchive = destinationArchive;
		this.filterProvider = filterProvider;
	}
	
	
	public IStatus fullPublishModule(IProgressMonitor monitor) throws CoreException {
		IStatus[] status = fullPublish(monitor);
		TrueZipUtil.umount();
		IStatus finalStatus = createModuleStatus(module, status);
		return finalStatus;
	}
	
	
	public IStatus incrementalPublishModule(IProgressMonitor monitor) throws CoreException {
		File destFile = destinationArchive.toFile();
		if( !destFile.exists() || destFile.isDirectory()) {
			return fullPublishModule(monitor);
		}
		
		
		String name = "Compressing " + lastModule().getName();
		monitor.beginTask(name, 1000);
		monitor.setTaskName(name);
		IModule[] moduleAsArray = module;
		IStatus[] operationStatus;
		
		ArrayList<IStatus> results = new ArrayList<IStatus>(); 
		int changeCount = countChanges(getDeltaForModule(module));
		if( changeCount > 0) {
			IProgressMonitor changeMonitor = ProgressMonitorUtil.submon(monitor, 300);
			changeMonitor.beginTask("Copying changed resources", changeCount * 100);
			results.addAll(Arrays.asList(publishChanges(moduleAsArray, changeMonitor)));
			changeMonitor.done();
		}
		
		List<IModule[]> removed = getRemovedChildModules();
		IModule[] children = getChildModules(moduleAsArray);
		if( children !=null)
			results.addAll(Arrays.asList(handleChildrenDeltas(moduleAsArray, children, ProgressMonitorUtil.submon(monitor, 600))));
		
		
		IProgressMonitor removedMonitor = ProgressMonitorUtil.submon(monitor, 100);
		removedMonitor.beginTask("Deleting removed modules", removed.size() * 100);
		Iterator<IModule[]> i = removed.iterator();
		while(i.hasNext() && !monitor.isCanceled()) {
			results.addAll(Arrays.asList(removeModule(i.next())));
			removedMonitor.worked(100);
		}
		operationStatus = (IStatus[]) results.toArray(new IStatus[results.size()]);
		removedMonitor.done();
		
		
		TrueZipUtil.umount();
		
		IStatus finalStatus = createModuleStatus(moduleAsArray, operationStatus);

		monitor.done();
		return finalStatus;
	}

	protected IModule[] getChildModules(IModule[] parent) {
		return server.getChildModules(parent, new NullProgressMonitor());
	}
	
	private IStatus[] removeModule(IModule[] module) {
		IPath moduleAbsoluteDestination = (module.length == 1 ? destinationArchive : destinationArchive.append(getRootModuleRelativePath(module)));
		boolean success = TrueZipUtil.deleteAll(moduleAbsoluteDestination);
		if( !success) {
			generateDeleteFailedStatus(moduleAbsoluteDestination.toFile());
		}
		return new IStatus[]{Status.OK_STATUS};
	}
	

	private IStatus[] fullPublish(IProgressMonitor monitor) {
		FileUtil.safeDelete(destinationArchive.toFile(), null);
		TrueZipUtil.umount();
		return fullPublish(module, null,ProgressMonitorUtil.getMonitorFor(monitor));
	}
	
	private IStatus[] fullPublish(IModule[] module, File parent, IProgressMonitor monitor) {
		monitor.beginTask("Packaging Module: " + module[module.length-1].getName(), 2000);
		
		ArrayList<IStatus> results = new ArrayList<IStatus>();
		
		IPath moduleAbsoluteDestination = (module.length == 1 ? destinationArchive : destinationArchive.append(getRootModuleRelativePath(module)));
		TFile moduleRoot = null;
		try {
			if( parent == null ) {
				moduleRoot = TrueZipUtil.getFile(moduleAbsoluteDestination, TrueZipUtil.getJarArchiveDetector());
				if( TrueZipUtil.pathExists(moduleRoot)) {
					TrueZipUtil.deleteAll(moduleRoot);
				}
				boolean createWorked = TrueZipUtil.createArchive(moduleAbsoluteDestination);
				moduleRoot = TrueZipUtil.getFile(moduleAbsoluteDestination, TrueZipUtil.getJarArchiveDetector());
			} else {
				IPath parentPath = new Path(parent.getAbsolutePath());
				IPath addition = moduleAbsoluteDestination.removeFirstSegments(parentPath.segmentCount());
				TrueZipUtil.createArchive(parent,addition);
				moduleRoot = TrueZipUtil.getRelativeArchiveFile(parent, addition);
			}
			monitor.worked(100);
			
			IModuleResource[] resources = getResources(module);
			IModulePathFilter filter = filterProvider == null ? null : filterProvider.getFilter(server, module);
			IModuleResource[] resources2 = filter == null ? resources : filter.getFilteredMembers();
			
			int totalCount = countMembers(resources2, true);
			IProgressMonitor copyMonitor = ProgressMonitorUtil.submon(monitor, 900);
			copyMonitor.beginTask("Copying Resources", totalCount*100);
			IStatus[] copyResults = copy(moduleRoot, resources2, copyMonitor);
			results.addAll(Arrays.asList(copyResults));
			copyMonitor.done();
			
			TrueZipUtil.umount();
			
			IModule[] children = getChildModules(module);
			if( children != null )
				publishChildren(module, results, children, moduleRoot,  ProgressMonitorUtil.submon(monitor, 1000));
			TrueZipUtil.umount();
		} catch( CoreException ce) {
			results.add(generateCoreExceptionStatus(ce));
		}
		monitor.done();
		return (IStatus[]) results.toArray(new IStatus[results.size()]);
	}

	
	private TFile getFile(IModule[] tree) {

		IPath tail = getRootModuleRelativePath(tree);
		IPath tailLocation = tail.removeLastSegments(1);

		TFile root = TrueZipUtil.getFile(destinationArchive, TrueZipUtil.getJarArchiveDetector());
		for( int i = 1; i < tree.length; i++ ) {
			String relative = ServerModelUtilities.getModuleParentRelativePath(tree, i);
			IPath relativePath = new Path(relative);
			String[] segments = relativePath.segments();
			for( int j = 0; j < segments.length - 1; j++ ) {
				root = TrueZipUtil.getFile(root, segments[j], TrueZipUtil.getNullArchiveDetector());
			}
			root = TrueZipUtil.getFile(root, segments[segments.length-1], TrueZipUtil.getJarArchiveDetector());
		}
		return root;
	}
	
	private IStatus[] fullBinaryPublish(IModule[] parent, IModule last, IProgressMonitor monitor) {

		TFile root  = getFile(combine(parent, last));
		ArrayList<IStatus> results = new ArrayList<IStatus>();
		try {
			
			IModuleResource[] resources = getResources(last, new NullProgressMonitor());
			int total = countMembers(resources, true);
			monitor.beginTask("Copying Resources", total*100);
			
			
			if( total != 1 || (total == 1 && !(resources[0] instanceof IModuleFile))) {
				results.addAll(Arrays.asList(copy(root, resources, monitor)));
			} else if( total == 1 ) {
				IModuleResource r1 = resources[0];
				IModuleFile mf = (IModuleFile)r1;
				java.io.File source = getFile(mf);
				if( source != null ) {
					boolean b = TrueZipUtil.archiveCopyAllTo(source, TrueZipUtil.getNullArchiveDetector(), root);
					if( !b )
						results.add(generateCopyFailStatus(source, root));
				}
			}
			monitor.done();
			TrueZipUtil.umount();
			return (IStatus[]) results.toArray(new IStatus[results.size()]);
		} catch( CoreException ce) {
			results.add(generateCoreExceptionStatus(ce));
			return (IStatus[]) results.toArray(new IStatus[results.size()]);
		}
	}

	private void publishChildren(IModule[] module, ArrayList<IStatus> results, IModule[] children, File parentModule, IProgressMonitor monitor) {
		if( children == null )
			return;
		monitor.beginTask("Assembling child modules", children.length * 100);
		for( int i = 0; i < children.length && !monitor.isCanceled(); i++ ) {
			if( monitor.isCanceled()) {
				results.add(new Status(IStatus.CANCEL, ASWTPToolsPlugin.PLUGIN_ID, "Operation Canceled"));
				return;
			}
			if( ServerModelUtilities.isBinaryModule(children[i]))
				results.addAll(Arrays.asList(fullBinaryPublish(module, children[i], ProgressMonitorUtil.submon(monitor, 100))));
			else
				results.addAll(Arrays.asList(fullPublish(combine(module, children[i]), parentModule, ProgressMonitorUtil.submon(monitor, 100))));
		}
	}
	
	
	
	private IStatus createModuleStatus(IModule[] module, IStatus[] operationStatus) {
		IStatus finalStatus;
		if( operationStatus.length > 0 ) {
			MultiStatus ms = new MultiStatus(ASWTPToolsPlugin.PLUGIN_ID, IEventCodes.JST_PUB_INC_FAIL, 
					"Publish Failed for module " + module[0].getName(), null);
			for( int i = 0; i < operationStatus.length; i++ )
				ms.add(operationStatus[i]);
			finalStatus = ms;
		}  else {
			finalStatus = new Status(IStatus.OK, ASWTPToolsPlugin.PLUGIN_ID, 
					IEventCodes.JST_PUB_FULL_SUCCESS, 
					NLS.bind(Messages.ModulePublished, module[0].getName()), null);
		}
		return finalStatus;
	}
	
	private List<IModule[]> getRemovedChildModules() {
		ArrayList<IModule[]> working = new ArrayList<IModule[]>();
		working.addAll(getRemovedModules());
		Iterator<IModule[]> i = working.iterator();
		while(i.hasNext()) {
			IModule[] removedArray = i.next();	
			IModule[] moduleAsArray = module;
			if( removedArray.length == 1 || !removedArray[0].getId().equals(lastModule().getId())) {
				i.remove();
			}
		}
		return working;
	}
	
	private IStatus[] handleChildrenDeltas(IModule[] module, IModule[] children, IProgressMonitor monitor) {
		monitor.beginTask("Handling Child Modules",  children.length * 100);
		ArrayList<IStatus> results = new ArrayList<IStatus>(); 
		for( int i = 0; i < children.length; i++ ) {
			IModule[] combinedChild = combine(module, children[i]);
			if( !hasBeenPublished(combinedChild)) {
				results.addAll(Arrays.asList(fullPublish(combinedChild, null, ProgressMonitorUtil.submon(monitor, 100))));
			}
			else if( isRemoved(combinedChild)) {
				results.addAll(Arrays.asList(removeModule(combinedChild)));
				monitor.worked(100);
			}
			else {
				results.addAll(Arrays.asList(publishChanges(combinedChild, ProgressMonitorUtil.submon(monitor, 25))));
				
				IModule[] children2 = getChildModules(combinedChild);
				if( children2 != null )
					results.addAll(Arrays.asList(handleChildrenDeltas(module, children2, ProgressMonitorUtil.submon(monitor, 75))));
			}
		}
		monitor.done();
		return (IStatus[]) results.toArray(new IStatus[results.size()]);
	}
	
	private boolean isRemoved(IModule[] child) {
		List<IModule[]> removed = getRemovedModules();
		Iterator<IModule[]> i = removed.iterator();
		while(i.hasNext()) {
			IModule[] next = i.next();
			if( next.length == child.length) {
				for( int j = 0; j < next.length; j++ ) {
					if( !next[j].getId().equals(child[j].getId())) {
						continue;
					}
				}
				return true;
			}
		}
		return false;
	}
	

	private IStatus[] publishChanges(IModule[] module, IProgressMonitor monitor) {
		IPath path = destinationArchive.append(getRootModuleRelativePath(module));
		File root = TrueZipUtil.getFile(path, TrueZipUtil.getJarArchiveDetector());
		IModuleResourceDelta[] deltas = getDeltaForModule(module);
		IModulePathFilter filter = filterProvider == null ? null : filterProvider.getFilter(server, module);
		return publishChanges(deltas, root, filter, monitor);
	}
	
	private IPath getRootModuleRelativePath(IModule[] module) {
		int start = this.module.length - 1;
		IModule[] toCheck = new IModule[module.length - start];
		for(int i = 0; i < module.length - start; i++ ) {
			toCheck[i] = module[start+i];
		}
		IPath ret = ServerModelUtilities.getRootModuleRelativePath(server, toCheck);
		return ret;
	}
	
	
	private IStatus[] publishChanges(IModuleResourceDelta[] deltas, 
			File root, IModulePathFilter filter, IProgressMonitor monitor) {
		ArrayList<IStatus> results = new ArrayList<IStatus>();
		if( deltas == null || deltas.length == 0 )
			return new IStatus[]{};
		int dKind;
		IModuleResource resource;
		for( int i = 0; i < deltas.length; i++ ) {
			dKind = deltas[i].getKind();
			resource = deltas[i].getModuleResource();
			if( dKind == IModuleResourceDelta.ADDED ) {
				if( filter == null || filter.shouldInclude(resource)) {
					results.addAll(Arrays.asList(copy(root, new IModuleResource[]{resource}, monitor)));
				}
			} else if( dKind == IModuleResourceDelta.CHANGED ) {
				if( filter == null || filter.shouldInclude(resource)) {
					if( resource instanceof IModuleFile ) 
						results.addAll(Arrays.asList(copy(root, new IModuleResource[]{resource}, monitor)));
					results.addAll(Arrays.asList(publishChanges(deltas[i].getAffectedChildren(), root, filter, monitor)));
				}
			} else if( dKind == IModuleResourceDelta.REMOVED) {
				File f = getDestinationJar(root, 
						resource.getModuleRelativePath().append(
								resource.getName()));
				boolean b = TrueZipUtil.deleteAll(f);
				if( !b )
					results.add(generateDeleteFailedStatus(f));
			} else if( dKind == IModuleResourceDelta.NO_CHANGE  ) {
				results.addAll(Arrays.asList(publishChanges(deltas[i].getAffectedChildren(), root, filter, monitor)));
			}
		}
		
		return (IStatus[]) results.toArray(new IStatus[results.size()]);
	}

	
	private IStatus[] copy(File root, IModuleResource[] children, IProgressMonitor monitor) {
		ArrayList<IStatus> results = new ArrayList<IStatus>();
		for( int i = 0; i < children.length; i++ ) {
			if( monitor.isCanceled()) { 
				results.add(new Status(IStatus.CANCEL, ASWTPToolsPlugin.PLUGIN_ID, "Operation Canceled"));
				return (IStatus[]) results.toArray(new IStatus[results.size()]);
			}
			
			if( children[i] instanceof IModuleFile ) {
				IModuleFile mf = (IModuleFile)children[i];
				File source = getFile(mf);
				if( source != null ) {
					File destination = getDestinationJar(root, mf.getModuleRelativePath().append(mf.getName()));
					boolean b = TrueZipUtil.archiveCopyAllTo(source, TrueZipUtil.getNullArchiveDetector(), destination);
					if( !b )
						results.add(generateCopyFailStatus(source, destination));
				}
				monitor.worked(100);
			} else if( children[i] instanceof IModuleFolder ) {
				File destination = getDestinationJar(root, children[i].getModuleRelativePath().append(children[i].getName()));
				destination.mkdirs();
				IModuleFolder mf = (IModuleFolder)children[i];
				results.addAll(Arrays.asList(copy(root, mf.members(), monitor)));
			}
		}
		return (IStatus[]) results.toArray(new IStatus[results.size()]);
	}
	
	private IStatus generateDeleteFailedStatus(File file) {
		return new Status(IStatus.ERROR, ASWTPToolsPlugin.PLUGIN_ID, "Could not delete file " + file);
	}
	private IStatus generateCoreExceptionStatus(CoreException ce) {
		return new Status(IStatus.ERROR, ASWTPToolsPlugin.PLUGIN_ID, ce.getMessage(), ce);
	}
	private IStatus generateCopyFailStatus(File source, File destination) {
		return new Status(IStatus.ERROR, ASWTPToolsPlugin.PLUGIN_ID, "Copy of " + source + " to " + destination + " has failed");
	}
	
	private File getDestinationJar(File root, IPath relative) {
		return TrueZipUtil.getDestinationJar(root, relative);
	}
	
	private TFile getFileInArchive(TFile root, IPath relative) {
		return TrueZipUtil.getFileInArchive(root, relative);
	}
	
	protected IModuleResourceDelta[] getDeltaForModule(IModule[] module) {
		IModuleResourceDelta[] deltas = ((Server)server).getPublishedResourceDelta(module);
		return deltas;
	}
	
	protected List<IModule[]> getRemovedModules() {
		List<IModule[]> l = ((Server)server).getAllModules();
		int size = l.size();
		((Server)server).getServerPublishInfo().addRemovedModules(l);
		if( l.size() > size ) {
			List<IModule[]> l2 = l.subList(size, l.size()-1);
			return l2;
		}
		return new ArrayList<IModule[]>();
	}
	
	protected boolean hasBeenPublished(IModule[] mod) {
		return ((Server)server).getServerPublishInfo().hasModulePublishInfo(mod);
	}

	public int childPublishTypeRequired() {
		return childPublishTypeRequired(module);
	}
	
	private IModule lastModule() {
		return module == null ? null : module.length == 0 ? null : module[module.length-1];
	}
	
	protected int childPublishTypeRequired(IModule[] mod) {
		IModule[] children = getChildModules(mod);
		boolean atLeastIncremental = false;
		for( int i = 0; i < children.length; i++ ) {
			IModule[] combinedChild = combine(mod, children[i]);
			if( !hasBeenPublished(combinedChild)) {
				return PublishControllerUtil.FULL_PUBLISH;
			}
			else if( isRemoved(combinedChild)) {
				return PublishControllerUtil.FULL_PUBLISH;
			}
			else {
				int childrenRequire = childPublishTypeRequired(combinedChild);
				if( childrenRequire == PublishControllerUtil.FULL_PUBLISH ) {
					return childrenRequire;
				}
				if( childrenRequire == PublishControllerUtil.INCREMENTAL_PUBLISH) {
					atLeastIncremental = true;
				}
				IModuleResourceDelta[] delta = getDeltaForModule(combinedChild);
				if( delta.length > 0 )
					atLeastIncremental = true;
			}
		}
		
		if( atLeastIncremental ) 
			return PublishControllerUtil.INCREMENTAL_PUBLISH;
		
		return PublishControllerUtil.NO_PUBLISH;
	}
	
}
