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
package org.jboss.ide.eclipse.archives.core;


import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

public class ArchivesCorePlugin extends Plugin {

	public static final String PLUGIN_ID = ArchivesCore.PLUGIN_ID;

	private static ArchivesCorePlugin plugin;
	private static BundleContext context;

	public ArchivesCorePlugin() {
		plugin = this;
	}

	public void start(BundleContext context2) throws Exception {
		super.start(context2);
		context = context2;

		new WorkspaceArchivesCore();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new WorkspaceChangeListener());
	}

	public ClassLoader getBundleClassLoader() {
		Bundle bundle = context.getBundle();
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		ClassLoader bundleLoader = bundleWiring.getClassLoader();
		return bundleLoader;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static ArchivesCorePlugin getDefault() {
		return plugin;
	}
}
