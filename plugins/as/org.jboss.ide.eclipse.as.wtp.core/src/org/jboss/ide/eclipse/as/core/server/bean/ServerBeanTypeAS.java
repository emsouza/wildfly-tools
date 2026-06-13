/******************************************************************************* 
 * Copyright (c) 2013 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.server.bean;

import java.io.File;

import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;

public class ServerBeanTypeAS extends JBossServerType {

	private static final String TWIDDLE_JAR_NAME = "twiddle.jar"; //$NON-NLS-1$
	protected ServerBeanTypeAS() {
		super(
				"AS",
				"JBoss Application Server", //$NON-NLS-1$
				asPath(BIN_PATH, TWIDDLE_JAR_NAME),
				new String[]{}, 
				new ASServerTypeCondition());
	}
	public static class ASServerTypeCondition extends AbstractCondition {
		public boolean isServerRoot(File location) {
			File asSystemJar = new File(location, JBossServerType.AS.getSystemJarPath());
			if (asSystemJar.exists() && asSystemJar.isFile()) {
				String title = getJarProperty(asSystemJar, IMPLEMENTATION_TITLE);
				boolean isEAP = title != null && title.contains("EAP"); //$NON-NLS-1$
				return !isEAP;
			}
			return false;
		}

		public String getServerTypeId(String version) {
			return null;
		}
	}
}
