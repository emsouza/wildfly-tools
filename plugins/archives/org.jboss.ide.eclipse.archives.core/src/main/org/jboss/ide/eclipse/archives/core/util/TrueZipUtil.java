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
package org.jboss.ide.eclipse.archives.core.util;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;

public class TrueZipUtil {

	public static TFile getFile(IPath path) {
		return getFile(path, TFile.getDefaultArchiveDetector());
	}

	public static TFile getFile(java.io.File f, TArchiveDetector detector) {
		return new TFile(f, detector);
	}

	public static TFile getFile(java.io.File f, String segment, TArchiveDetector detector) {
		return new TFile(f, segment, detector);
	}

	public static TFile getFile(IPath path, TArchiveDetector detector) {
		return new TFile(path.toOSString(), detector);
	}

	public static boolean pathExists(IPath path) {
		return pathExists(getFile(path));
	}

	public static boolean pathExists(TFile file) {
		return file.exists();
	}

	public static long getTimestamp(IPath path) {
		return getTimestamp(getFile(path));
	}

	public static long getTimestamp(TFile file) {
		return file.lastModified();
	}

	public static boolean copyFile(String source, IPath dest) throws IOException {
		return copyFile(source, getFile(dest), true);
	}

	public static boolean copyFile(String source, TFile file, boolean updateTimestamps) {
		try {
			file.getParentFile().mkdirs();
			TFile srcFile = new TFile(source, TArchiveDetector.NULL);
			file.cp_r(srcFile);
			return updateTimestamps ? updateParentTimestamps(file) : true;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean touchFile(IPath path) {
		try {
			TFile f = getFile(path);
			boolean b = f.setLastModified(System.currentTimeMillis());
			return b && updateParentTimestamps(path);
		} catch (Exception e) {
			return false;
		}
	}

	// Delete methods
	public static boolean deleteAll(IPath path, String fileName) {
		return deleteAll(path.append(fileName));
	}

	public static boolean deleteAll(IPath path) {
		return deleteAll(getFile(path));
	}

	public static boolean deleteAll(TFile file) {
		try {
			TFile.rm_r(file);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean deleteEmptyChildren(java.io.File file) {
		boolean b = true;
		if (file.isDirectory()) {
			java.io.File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
				b &= deleteEmptyFolders(children[i]);
		}
		return b;
	}

	public static boolean deleteEmptyFolders(java.io.File file) {
		boolean b = true;
		if (file.isDirectory()) {
			java.io.File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
				b &= deleteEmptyFolders(children[i]);
			if (file.listFiles().length == 0)
				file.delete();
		}
		return b;
	}

	public static boolean createFolder(IPath parent, String folderName) {
		TFile parentFile = getFile(parent, TFile.getDefaultArchiveDetector());
		boolean b = new TFile(parentFile, folderName, TArchiveDetector.NULL).mkdirs();
		return b && updateParentTimestamps(parent.append(folderName));
	}

	public static boolean createFolder(IPath path) {
		return createFolder(path.removeLastSegments(1), path.lastSegment());
	}

	public static boolean createArchive(IPath parent, String folderName) {
		TFile parentFile = getFile(parent, TFile.getDefaultArchiveDetector());
		boolean b = new TFile(parentFile, folderName, getJarArchiveDetector()).mkdirs();
		return b && updateParentTimestamps(parent.append(folderName));
	}

	public static boolean createArchive(IPath path) {
		return createArchive(path.removeLastSegments(1), path.lastSegment());
	}

	public static boolean createArchive(java.io.File parentFile, IPath relative) {
		TFile archive = getRelativeArchiveFile(parentFile, relative);
		boolean b = archive.mkdirs();
		return b && updateParentTimestamps(archive);
	}

	public static TFile getRelativeArchiveFile(java.io.File parentFile, IPath relative) {
		return getRelativeArchiveFileInternal(parentFile, relative);
	}

	private static TFile getRelativeArchiveFileInternal(java.io.File parentFile, IPath relative) {
		TFile working;
		if (parentFile instanceof TFile)
			working = (TFile) parentFile;
		else
			working = new TFile(parentFile);

		if (relative.segmentCount() == 0)
			return working;

		IPath finalFileRelativeLocationPath = relative.removeLastSegments(1);
		TFile finalFileLocation = getFileInArchive(working, finalFileRelativeLocationPath);

		TFile retval = new TFile(finalFileLocation, relative.lastSegment(), getJarArchiveDetector());
		return retval;
	}

	public static void umount() {
		try {
			TFile.umount();
		} catch (Exception e) {
		}
	}

	public static void syncExec(Runnable run) {
		try {
			if (run != null)
				run.run();
		} catch (Exception e) {
		}
		umount();
	}

	public static void sync() {
		syncExec(null);
	}

	public static boolean updateParentTimestamps(IPath path) {
		return updateParentTimestamps(getFile(path));
	}

	public static boolean updateParentTimestamps(TFile file) {
		long time = System.currentTimeMillis();
		TFile parent = file.getParentFile();
		boolean b = true;
		while (parent != null) {
			b &= parent.setLastModified(time);
			parent = parent.getEnclArchive();
		}
		return b;
	}

	public static TArchiveDetector getJarArchiveDetector() {
		return TArchiveDetector.ALL;
	}

	public static TArchiveDetector getNullArchiveDetector() {
		return TArchiveDetector.NULL;
	}

	public static TArchiveDetector getDefaultArchiveDetector() {
		return TFile.getDefaultArchiveDetector();
	}

	public static boolean archiveCopyAllTo(java.io.File source, TArchiveDetector detector, java.io.File destination) {
		try {
			TFile srcFile = new TFile(source, detector);
			TFile destFile = new TFile(destination, TArchiveDetector.NULL);
			destFile.cp_r(srcFile);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean javaIODeleteDir(java.io.File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = javaIODeleteDir(new java.io.File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	public static TFile getFileInArchive(TFile root, IPath relative) {
		while (relative.segmentCount() > 0) {
			root = new TFile(root, relative.segment(0), TArchiveDetector.NULL);
			relative = relative.removeFirstSegments(1);
		}
		return root;
	}

	public static java.io.File getDestinationJar(java.io.File root, IPath relative) {
		while (relative.segmentCount() > 0) {
			if (relative.segmentCount() == 1) {
				root = new TFile(root, relative.segment(0), TArchiveDetector.ALL);
			} else {
				root = new TFile(root, relative.segment(0), TArchiveDetector.NULL);
			}
			relative = relative.removeFirstSegments(1);
		}
		return root;
	}

	public static boolean deleteAll(java.io.File f) {
		if (f instanceof TFile) {
			try {
				TFile.rm_r(f);
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		return f.delete();
	}
}
