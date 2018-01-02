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

import org.apache.log4j.Logger;
import org.ihtsdo.rf2.snapshot.SnapshotManager;


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
			
//			String date="20180131";
//			String pathId="214";
//			String executionId="1";
//			String db="en-edition";
//			String defaultSnapshotFolder="/Users/ar/classifier/en-edition/214/exported-snapshot";
//			String defaultLangCode="en";
//			String moduleId="900000000000207008";
//			String namespace="0";
			
			SnapshotManager sm=new SnapshotManager(file, db,pathId, date, defaultSnapshotFolder, moduleId, namespace);
			sm.execute();		
			sm=null;
			
			boolean classified=true;
			CycleCheck cc=new CycleCheck(file,db,pathId,executionId, defaultSnapshotFolder,defaultLangCode);
			if (cc.cycleDetected()){
				classified=false;
			}
			cc=null;
					
			if (classified){
				ClassificationRunner cr=new ClassificationRunner(file,date,db,pathId,executionId,defaultSnapshotFolder,defaultLangCode);
				cr.execute();
				cr=null;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
		System.exit(0);
	}
}
