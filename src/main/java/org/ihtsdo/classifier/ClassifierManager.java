/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.classifier;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.utils.FileHelper;
import org.ihtsdo.classifier.utils.I_Constants;
import org.ihtsdo.rf2.snapshot.SnapshotZipRunner;
import org.snomed.otf.owltoolkit.service.SnomedReasonerService;


/**
 * The Class ClassifierManager.
 * This class has main method to run the classifier and the cycle detector.
 * Params are: -CC to run cycle detector, -CR to run classifier, {file} file containing params  
 *
 * @author Alejandro Rodriguez.
 *
 * @version 1.0
 */
public class ClassifierManager {

	private static Logger logger;

	public static void main(String[] args){

		logger = Logger.getLogger("org.ihtsdo.classifier.ClassifierManager");
		try {
			if (args==null || args.length<1){
				logger.info("Error happened getting params. Params file doesn't exist");
				System.exit(0);
			}
			File file =new File(args[0]);
			if (!file.exists()){
				logger.info("Error happened getting params. Params file doesn't exist");
				System.exit(0);
			}
			String date=args[1];
			String pathId=args[2];
			String executionId=args[3];
			String db=args[4];
			String defaultSnapshotFolder=args[5];
			String defaultLangCode=args[6];
			String moduleId=args[7];
			String namespace=args[8];
			String server=args[9];

//			String date="20210331";
//			String pathId="112";
//			String executionId="1";
//			String db="nl-edition";
//			String defaultSnapshotFolder="/Users/ar/Downloads/7c6b8292-c901-54c1-bf12-60ce0ab56923";
//			String defaultLangCode="en";
//			String moduleId="11000146104";
//			String namespace="1000146";
//			String server="localhost";

			String classifierFolder=getClassifierFolder(file);
			File baseFolder=new File( classifierFolder + "/" + db + "/" + pathId.toString());
			File reportFolder=new File( classifierFolder + "/" + server + "/" + db + "/" + moduleId + "/" + pathId.toString());
			if (!reportFolder.exists()){
			    reportFolder.mkdirs();
            }
            FileHelper.emptyFolder(reportFolder);
			SnapshotZipRunner zm=new SnapshotZipRunner(baseFolder, date, defaultSnapshotFolder, moduleId, namespace);
			zm.execute();
			zm=null;

			File zipPrevSnapshot=new File(baseFolder,I_Constants.CLASSIFIER_PREVIOUS_SNAP_ZIP_FILE);
			Set<File> snapshotFiles = new HashSet<File>();
			snapshotFiles.add(zipPrevSnapshot);

			File zipExportedDelta=new File(baseFolder,I_Constants.CLASSIFIER_EXPORTED_DELTA_ZIP_FILE);

			String fileName="classification-results_" + date ;
			File resultsFile = new File(baseFolder,fileName + ".zip");
            if (resultsFile.exists()){
                resultsFile.delete();
            }
			CycleCheck cc=new CycleCheck(file,reportFolder, db, pathId,executionId,
					defaultSnapshotFolder,defaultLangCode);
			if (!cc.cycleDetected()) {
				cc = null;
				new SnomedReasonerService().classify(
						"command-line",
						snapshotFiles,
						zipExportedDelta,
						resultsFile,
						SnomedReasonerService.ELK_REASONER_FACTORY,
						false // outputOntologyFileForDebug
				);

				System.out.println("Classification results written to " + resultsFile.getAbsolutePath());

				PostClassificationRunner pcr = new PostClassificationRunner(baseFolder, resultsFile, moduleId, date, executionId, defaultSnapshotFolder, defaultLangCode,reportFolder);
				pcr.execute();
				pcr = null;
			}else{
				cc=null;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
		System.exit(0);
	}

	public static String getClassifierFolder(File config) throws ConfigurationException {
		XMLConfiguration xmlConfig;
		try {
			xmlConfig=new XMLConfiguration(config);
		} catch (ConfigurationException e) {
			logger.info("ClassificationRunner - Error happened getting params file." + e.getMessage());
			throw e;
		}

		String classifierFolder=xmlConfig.getString(I_Constants.CLASSIFIERFOLDER);
		xmlConfig=null;
		return classifierFolder;
	}
}
