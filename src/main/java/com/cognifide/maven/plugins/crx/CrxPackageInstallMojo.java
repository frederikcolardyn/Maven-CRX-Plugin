/*
 * Copyright 2012 Cognifide Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.cognifide.maven.plugins.crx;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;

/**
 * Installs an CRX package to a running CRX instance.
 * 
 * @goal install
 * @description Installs an CRX package to a running CRX instance.
 * 
 * @execute goal='upload'
 */
public class CrxPackageInstallMojo extends CrxPackageAbstractMojo {

	/**
	 * @parameter expression="${packagePath}"
	 * 
	 * @readonly
	 */
	private String packagePath;

	/**
	 * Don't exit if the install was a success but with errors.
	 *
	 * @parameter expression="${crx.ignoreSuccessErrors}" default-value="false"
	 */
	protected boolean ignoreSuccessErrors;

	private String uploadedPackagePath;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (runOnlyAtExecutionRoot && !isThisTheExecutionRoot()) {
			getLog().info("Skipping upload in this project because it's not the Execution Root");
			return;
		}
		
		if (this.skip) {
			getLog().info("Skipping package installation as instructed");
			return;
		}

		uploadedPackagePath = StringUtils.defaultString(mavenSession.getUserProperties().get(
				UPLOADED_PACKAGE_PATH_PROPERTY));
		
		if (StringUtils.isEmpty(uploadedPackagePath)) {
			uploadedPackagePath = packagePath;
		}

		if (StringUtils.isEmpty(uploadedPackagePath)) {
			getLog().error("Uploading goal hasn't set up path for package!");
			return;
		}

		String targetURL = getInstallURL(uploadedPackagePath);

		getLog().info("Installing package using command: " + targetURL);
		String response = post(targetURL);

		HtmlParser parser = new HtmlParser(response);
		parser.parse();

		switch (parser.getStatus()) {
			case SUCCESS:
				if (parser.getErrors().size() == 0) {
					getLog().info("Package successfully installed.");
				} else {
					getLog().warn("Package installed with errors");
					displayErrors(parser.getErrors());
					if (!ignoreSuccessErrors){
						throw new MojoExecutionException("Installation completed with errors!");
					}
				}
				break;
			case SUCCESS_WITH_ERRORS:
				getLog().error("Package installed with errors.");
				displayErrors(parser.getErrors());
				if (!ignoreSuccessErrors){
					throw new MojoExecutionException("Installation completed with errors!");
				}
				break;
			case FAIL:
				getLog().error("Installation failed.");
				displayErrors(parser.getErrors());
				throw new MojoExecutionException("Installation incomplete!");
		}
		return;
	}

	protected String getInstallURL(String path) {
		return getHtmlTargetURL() + path + "/?cmd=install";
	}

	protected void displayErrors(List<String> errors) {
		for (String error : errors) {
			getLog().error("CRX: " + error);
		}
	}
}
