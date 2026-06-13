/*******************************************************************************
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.archives.core.util.internal;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.jboss.ide.eclipse.archives.core.ArchivesCore;
import org.jboss.ide.eclipse.archives.core.ArchivesCoreMessages;
import org.jboss.ide.eclipse.archives.core.model.IArchive;
import org.jboss.ide.eclipse.archives.core.model.IArchiveFileSet;
import org.jboss.ide.eclipse.archives.core.model.IArchiveStandardFileSet;
import org.jboss.ide.eclipse.archives.core.model.IArchiveFolder;
import org.jboss.ide.eclipse.archives.core.model.IArchiveNode;
import org.jboss.ide.eclipse.archives.core.model.IArchiveNodeVisitor;
import org.jboss.ide.eclipse.archives.core.model.DirectoryScannerFactory.DirectoryScannerExtension.FileWrapper;
import org.jboss.ide.eclipse.archives.core.util.ModelUtil;
import org.jboss.ide.eclipse.archives.core.util.PathUtils;
import org.jboss.ide.eclipse.archives.core.util.TrueZipUtil;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

public class ModelTruezipBridge {
	public static class FileWrapperStatusPair {
		public FileWrapper[] f;
		public IStatus[] s;
		public FileWrapperStatusPair(FileWrapper[] files, IStatus[] statuses) {
			this.f = files;
			this.s = statuses;
		}
	}
	public static FileWrapperStatusPair fullFilesetBuild(final IArchiveFileSet fileset, IProgressMonitor monitor, boolean sync) {
		FileWrapper[] files = fileset.findMatchingPaths();
		IStatus[] s = copyFiles(fileset, files, monitor, false, false);
		if( sync )
			TrueZipUtil.sync();
		return new FileWrapperStatusPair( files, s );
	}

	public static class FullBuildRequiredException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public FullBuildRequiredException(String s, Exception cause) {
			super(s,cause);
		}
	}

	public static FileWrapperStatusPair fullFilesetRemove(final IArchiveFileSet fileset, IProgressMonitor monitor, boolean sync) {
		monitor.beginTask(ArchivesCore.bind(ArchivesCoreMessages.RemovingFileset,fileset.toString()), 2500);
		FileWrapper[] files = null;
		try {
			files = fileset.findMatchingPaths();
		} catch(RuntimeException re) {
			throw new FullBuildRequiredException("Unable to incrementally remove fileset. A full build may be required.", re);
		}
		final ArrayList<IStatus> errors = new ArrayList<IStatus>();
		final ArrayList<FileWrapper> list = new ArrayList<FileWrapper>();
		list.addAll(Arrays.asList(files));
		IProgressMonitor filesMonitor = new SubProgressMonitor(monitor, 2000);
		filesMonitor.beginTask( ArchivesCore.bind(
				ArchivesCoreMessages.RemovingCountFiles, new Integer(files.length).toString()), files.length * 100);
		for( int i = 0; i < files.length; i++ ) {
			if( !ModelUtil.otherFilesetMatchesPathAndOutputLocation(fileset, files[i])) {
				errors.addAll(Arrays.asList(deleteFiles(fileset, new FileWrapper[] {files[i]}, new NullProgressMonitor(), false)));
			} else {
				list.remove(files[i]);
			}
			filesMonitor.worked(100);
		}
		filesMonitor.done();

		TFile folder = getFile(fileset);
		if( !cleanFolder(folder, false) ) {
			IStatus e = new Status(IStatus.ERROR, ArchivesCore.PLUGIN_ID,
					ArchivesCore.bind(ArchivesCoreMessages.ErrorEmptyingFolder,folder.toString()));
			errors.add(e);
		}
		monitor.worked(250);

		fileset.getParent().accept(new IArchiveNodeVisitor() {
			public boolean visit(IArchiveNode node) {
				boolean b = true;
				if( node.getNodeType() == IArchiveNode.TYPE_ARCHIVE) {
					b = createFile(node);
				} else if( node.getNodeType() == IArchiveNode.TYPE_ARCHIVE_FOLDER) {
					b = createFile(node);
				}
				if( !b ) {
					IStatus e = new Status(IStatus.ERROR, ArchivesCore.PLUGIN_ID,
							ArchivesCore.bind(ArchivesCoreMessages.ErrorCreatingFile,getFile(node).toString()));
					errors.add(e);
				}
				return true;
			}
		} );

		if( sync )
			TrueZipUtil.sync();
		monitor.worked(250);
		monitor.done();

		IStatus[] errorsArr = errors.toArray(new IStatus[errors.size()]);
		FileWrapper[] files2 = list.toArray(new FileWrapper[list.size()]);
		return new FileWrapperStatusPair( files2, errorsArr);
	}

	public static IStatus[] copyFiles(IArchiveFileSet fileset, final FileWrapper[] files, IProgressMonitor monitor, boolean updateTimestamps, boolean sync) {
		monitor.beginTask(ArchivesCore.bind(ArchivesCoreMessages.CopyingCountFiles,
				new Integer(files.length).toString()), files.length * 100);
		boolean b = true;
		ArrayList<IStatus> list = new ArrayList<IStatus>();
		final TFile[] destFiles = getFiles(files, fileset);
		for( int i = 0; i < files.length; i++ ) {
			try {
				b = TrueZipUtil.copyFile(files[i].getAbsolutePath(), destFiles[i], updateTimestamps);
			} catch(Exception e) {
				b = false;
			}
			if( b == false ) {
				list.add(new Status(IStatus.ERROR, ArchivesCore.PLUGIN_ID,
						ArchivesCore.bind(ArchivesCoreMessages.FileCopyFailed,
								files[i].getAbsolutePath(), destFiles[i].toString())));
			}
			monitor.worked(100);
		}
		if( sync )
			TrueZipUtil.sync();
		monitor.done();
		return list.toArray(new IStatus[list.size()]);
	}

	public static IStatus[] deleteFiles(IArchiveFileSet fileset, final FileWrapper[] files, IProgressMonitor monitor, boolean sync ) {
		monitor.beginTask(ArchivesCore.bind(ArchivesCoreMessages.DeletingCountFiles,
				new Integer(files.length).toString()), files.length * 100);
		final TFile[] destFiles = getFiles(files, fileset);
		ArrayList<IStatus> list = new ArrayList<IStatus>();
		for( int i = 0; i < files.length; i++ ) {
			if( !TrueZipUtil.deleteAll(destFiles[i]) ) {
				IStatus e = new Status(IStatus.ERROR, ArchivesCore.PLUGIN_ID,
						ArchivesCore.bind(ArchivesCoreMessages.FileDeleteFailed, destFiles[i].toString()));
				list.add(e);
			}
			monitor.worked(100);
		}
		if( sync )
			TrueZipUtil.sync();
		monitor.done();
		return list.toArray(new IStatus[list.size()]);
	}

	public static boolean deleteArchive(IArchive archive) {
		final TFile file = getFile(archive);
		boolean b = TrueZipUtil.deleteAll(file);
		TrueZipUtil.sync();
		return b;
	}

	public static boolean cleanFolder(IArchiveFolder folder) {
		return cleanFolder(getFile(folder), true);
	}

	public static boolean cleanFolder(java.io.File folder, boolean sync) {
		boolean b = TrueZipUtil.deleteEmptyChildren(folder);
		if( sync )
			TrueZipUtil.sync();
		return b;
	}

	public static boolean createFile(final IArchiveNode node) {
		return createFile(node, true);
	}

	public static boolean createFile(final IArchiveNode node, boolean sync) {
		TFile f = getFile(node);
		if( f == null ) return false;
		if( f.exists() ) return true;
		boolean b = f.mkdirs();
		if( sync )
			TrueZipUtil.sync();
		return b;
	}

	private static TFile[] getFiles(FileWrapper[] inputFiles, IArchiveFileSet fs ) {
		String filesetRelative;
		TFile fsFile = getFile(fs);
		if( fs == null || fsFile == null )
			return new TFile[]{};

		ArrayList<TFile> returnFiles = new ArrayList<TFile>();
		for( int i = 0; i < inputFiles.length; i++ ) {
			if( inputFiles[i] == null )
				continue;

			if( fs instanceof IArchiveStandardFileSet && ((IArchiveStandardFileSet)fs).isFlattened() )
				filesetRelative = inputFiles[i].getOutputName();
			else
				filesetRelative = inputFiles[i].getFilesetRelative();

			TFile parentFile;
			if(new Path(filesetRelative).segmentCount() > 1 ) {
				String tmp = new Path(filesetRelative).removeLastSegments(1).toString();
				parentFile = new TFile(fsFile, tmp, TArchiveDetector.NULL);
				if( parentFile.getEnclArchive() != null )
					parentFile = new TFile(fsFile, tmp, TFile.getDefaultArchiveDetector());
			} else {
				parentFile = fsFile;
			}
			returnFiles.add(new TFile(parentFile, new Path(filesetRelative).lastSegment(), TArchiveDetector.NULL));
		}
		return returnFiles.toArray(new TFile[returnFiles.size()]);
	}

	private static TFile getFile(IArchiveNode node) {
		if( node == null ) return null;

		if( node.getNodeType() == IArchiveNode.TYPE_MODEL_ROOT ) return null;

		if( node.getNodeType() == IArchiveNode.TYPE_ARCHIVE_FILESET)
			return getFile(node.getParent());

		TFile parentFile = getFile(node.getParent());
		if( node.getNodeType() == IArchiveNode.TYPE_ARCHIVE ) {
			IArchive node2 = ((IArchive)node);
			boolean exploded = ((IArchive)node).isExploded();
			TArchiveDetector detector = exploded ? TArchiveDetector.NULL : TrueZipUtil.getJarArchiveDetector();
			if( parentFile == null ) {
				IPath p = PathUtils.getGlobalLocation(node2);
				if( p == null ) return null;
				parentFile = new TFile(p.toOSString(), TArchiveDetector.NULL);
			}
			return new TFile(parentFile, node2.getName(), detector);
		}
		if( node.getNodeType() == IArchiveNode.TYPE_ARCHIVE_FOLDER ) {
			return new TFile(parentFile, ((IArchiveFolder)node).getName(), TArchiveDetector.NULL);
		}
		return null;
	}

	public static String getFilePath(IArchiveNode node) {
		TFile f = getFile(node);
		return f == null ? null : f.getAbsolutePath();
	}
}
