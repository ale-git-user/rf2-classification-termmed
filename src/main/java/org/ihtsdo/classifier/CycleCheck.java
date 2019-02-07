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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.model.Changes;
import org.ihtsdo.classifier.model.Report;
import org.ihtsdo.classifier.utils.FileHelper;
import org.ihtsdo.classifier.utils.I_Constants;
import org.ihtsdo.json.model.ConceptDescriptor;
import org.ihtsdo.json.model.RelationshipVersion;

import com.google.gson.Gson;

/**
 * The Class CycleCheck.
 * This class is responsible to detect cyclic isa relationships from RF2 format.
 * Output results are conceptIds for detected cycled.
 * ConceptIds for detected cycled are saved in file which is a parameter of class constructor.
 *
 * @author Alejandro Rodriguez.
 *
 * @version 1.0
 */
public class CycleCheck {

	/** The concepts. */
	private HashMap<String, Boolean> concepts;

	/** The isa relationships map. */
	private HashMap<String, List<String>> isarelationships;

	/** The concept in loop. */
	private HashSet<String> conceptInLoop;

	/** The isa relationship typeid. */
	private String ISARELATIONSHIPTYPEID="116680003";

	/** The concept file. */
	private String[] conceptFile;

	/** The relationship file. */
	private String[] relationshipFile;

	/** The output file. */
	private String outputFile;

	/** The reviewed. */
	private int reviewed;

	private File config;

	private XMLConfiguration xmlConfig;

	/** The logger. */
	private  Logger logger;

	private String classifierFolder;

	private Object pathId;

	private String executionId;

	private String dbName;

	private String defaultSnapshotFolder;

	private String descriptions;

	private HashMap<String, String[]> defaultTerms;

	private String defaultLangCode;

	public CycleCheck(File config, String dbName, String pathId, String executionId, String defaultSnapshotFolder, String defaultLangCode) throws IOException, Exception {
		this.config=config;
		this.pathId=pathId;
		this.executionId=executionId;
		this.dbName=dbName;
		this.defaultSnapshotFolder=defaultSnapshotFolder;
		this.defaultLangCode=defaultLangCode;
		logger = Logger.getLogger("org.ihtsdo.classifier.CycleCheck");
		getParams();
	}

	private void getParams() throws IOException, Exception {

		try {
			xmlConfig=new XMLConfiguration(config);
		} catch (ConfigurationException e) {
			logger.info("CycleCheck - Error happened getting params file." + e.getMessage());
			throw e;
		}
		this.classifierFolder=xmlConfig.getString(I_Constants.CLASSIFIERFOLDER);
		File detectFolder=new File(classifierFolder + "/" + dbName + "/" + pathId.toString() + "/detectedCycles");
		if (!detectFolder.exists()){
			detectFolder.mkdirs();
		}
		outputFile=detectFolder.getAbsolutePath() + "/detectedCycles.txt";
		
		File tmpFile=new File(outputFile);
		if (tmpFile.exists()){
			tmpFile.delete();
		}
		File pathFolder=new File( classifierFolder + "/" + dbName + "/" + pathId.toString() + "/exported-snapshot");
		if (!pathFolder.exists()){
			pathFolder.mkdirs();
		}
		String conceptF=FileHelper.getFile( pathFolder, "rf2-concepts", classifierFolder + "/releasedFiles/" + dbName ,null,null);
		conceptFile=new String[]{conceptF};

		String descriptionFile=FileHelper.getFile( pathFolder, "rf2-descriptions", defaultSnapshotFolder,null,null);
		descriptions=descriptionFile;
		
		String staRelFile=FileHelper.getFile( pathFolder, "rf2-relationships", classifierFolder + "/releasedFiles/" + dbName ,"stated",null);
		relationshipFile=new String[]{staRelFile};


		logger.info("CheckCycle - Parameters:");
		logger.info("Concept files : ");
		for (String concept:conceptFile){
			logger.info( concept);
		}
		logger.info("Relationship files : " );
		for (String relFile:relationshipFile){
			logger.info(relFile);
		}
		logger.info("Detected cycle output file = " + outputFile);

	}

	/**
	 * Cycle detected.
	 *
	 * @return true, if successful
	 * @throws java.io.FileNotFoundException the file not found exception
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public boolean cycleDetected() throws FileNotFoundException, IOException{
		conceptInLoop=new HashSet<String>();
		loadConceptsFile();
		loadIsaRelationshipsFile();
		for(String con:concepts.keySet()){
			if (!concepts.get(con)){
				List<String> desc=new ArrayList<String>();
				findCycle(con, desc);
				desc.remove(con);
				reviewed++;
				concepts.put(con, true);
			}
		}
		if (conceptInLoop.size()>0){
			saveDetectedCyclesFile();
			logger.info("CYCLE DETECTED - Concepts reviewed: "  + reviewed );
			logger.info("Please get conceptId for detected cycles in file:" + outputFile);
			return true;
		}
		logger.info("*******NO CYCLE DETECTED***** - Concepts reviewed: "  + reviewed);
		return false;
	}

	/**
	 * Save conceptIds in detected cycles file.
	 *
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	private void saveDetectedCyclesFile() throws IOException {
		AddDefaultTerms(conceptInLoop);
		Gson gson=new Gson();
		FileOutputStream fos = new FileOutputStream( outputFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		Report report=new Report();
		report.setExecutionId(executionId);
		if (conceptInLoop.size()>0){
			report.setCyclesDetected("Yes");
		}else{
			report.setCyclesDetected("No");
		}
		List<ConceptDescriptor> concepts=new ArrayList<ConceptDescriptor>();
		for (String concept:conceptInLoop){
			ConceptDescriptor conceptDescriptor = getConceptDescriptor(concept);
			concepts.add(conceptDescriptor);
		}
		report.setConceptInCycles(concepts);
		report.setEquivalentConceptGroups(new ArrayList<ArrayList<ConceptDescriptor>>());
		report.setEquivalentsDefinitionsDetected("No");

		Changes changes=new Changes();
		changes.setNewInferredIsas(new ArrayList<RelationshipVersion>() );
		changes.setNewAttributes(new ArrayList<RelationshipVersion>() );
		changes.setLostInferredIsas(new ArrayList<RelationshipVersion>() );
		changes.setLostAttributes(new ArrayList<RelationshipVersion>() );
		
		report.setChanges(changes);
		
		
		bw.append(gson.toJson(report));
		bw.close();
		bw=null;
		fos=null;
		osw=null;

	}

	private ConceptDescriptor getConceptDescriptor(String conceptId){

		ConceptDescriptor cd=new ConceptDescriptor();
		cd.setConceptId(conceptId);
		cd.setDefinitionStatus("900000000000074008");

		String moduleC1=null;
		String term=null;
		String[] termModule=defaultTerms.get(conceptId);
		if (termModule!=null){
			moduleC1=termModule[1];
			term=termModule[0];
		}
			
		cd.setModule(moduleC1);
		cd.setDefaultTerm(term);
		return cd;
	}
	
	private void AddDefaultTerms(HashSet<String> tmpConcepts) throws IOException {
		if (defaultTerms==null){
			defaultTerms=new HashMap<String,String[]>();
		}

		FileInputStream rfis = new FileInputStream(descriptions);
		InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
		BufferedReader rbr = new BufferedReader(risr);
		rbr.readLine();
		String line;
		String[] spl;
		while((line=rbr.readLine())!=null){
			spl=line.split("\t",-1);
			if (tmpConcepts.contains(spl[4])){
				if (spl[2].equals("1")){
					if (spl[6].equals("900000000000003001")){
						if (spl[5].equals(defaultLangCode)){
							defaultTerms.put(spl[4], new String[]{spl[7],spl[3]});
						}else if (!defaultTerms.containsKey(spl[4])){
							defaultTerms.put(spl[4], new String[]{spl[7],spl[3]});
						}
					}else if (!defaultTerms.containsKey(spl[4])){
						if (spl[5].equals(defaultLangCode)){
							defaultTerms.put(spl[4], new String[]{spl[7],spl[3]});
						}
					}
				}
			}
		}
		rbr.close();
		rbr=null;
		rfis=null;
		risr=null;

	}

	/**
	 * Find cycle.
	 *
	 * @param con the con
	 * @param desc the desc
	 */
	private void findCycle(String con, List<String> desc) {
		List<String> parents=isarelationships.get(con);
		if (parents!=null){
			desc.add(con);
			for (String parent:parents){
				if (desc.contains(parent)){
					conceptInLoop.add(parent);
				}else{
					if (concepts==null){
						System.out.println("concepts set is null");
					}
					if (concepts.get(parent)==null){
						System.out.println("concepts.get(" + parent + ") is null");
						
					}
					if (!concepts.get(parent)){
						findCycle(parent,desc);
						desc.remove(parent);
						reviewed++;
						concepts.put(parent, true);
					}
				}
			}
		}
	}

	/**
	 * Load concepts file.
	 *
	 * @param conceptFile2 the concepts file
	 * @throws java.io.FileNotFoundException the file not found exception
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public void loadConceptsFile() throws FileNotFoundException, IOException {

		concepts=new HashMap<String, Boolean>();
		int count = 0;
		for (String concept:conceptFile){
			logger.info("Starting Concepts: " + concept);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(concept), "UTF8"));
			try {
				String line = br.readLine();
				line=br.readLine();
				while (line != null) {
					if (line.isEmpty()) {
						continue;
					}
					String[] columns = line.split("\t",-1);
					if ( columns[2].equals("1") ){

						concepts.put(columns[0], false);
						count++;
						if (count % 100000 == 0) {
							System.out.print(".");
						}
					}
					line = br.readLine();
				}

				logger.info(".");
				logger.info("Active concepts loaded = " + concepts.size());
			} finally {
				br.close();
			}
		}

	}




	/**
	 * Load isa relationships file.
	 *
	 * @throws java.io.FileNotFoundException the file not found exception
	 * @throws java.io.IOException Signals that an I/O exception has occurred.
	 */
	public void loadIsaRelationshipsFile() throws FileNotFoundException, IOException {

		isarelationships=new HashMap<String,List<String>>();
		int count = 0;
		for (String relFile:relationshipFile){
			logger.info("Starting Isas Relationships from: " + relFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(relFile), "UTF8"));
			try {
				String line = br.readLine();
				line=br.readLine();
				while (line != null) {
					if (line.isEmpty()) {
						continue;
					}
					String[] columns = line.split("\t");
					if (columns[7].equals(ISARELATIONSHIPTYPEID) 
							&& columns[2].equals("1") && concepts.containsKey(columns[5])){
						String sourceId = columns[4];

						List<String> relList = isarelationships.get(sourceId);
						if (relList == null) {
							relList = new ArrayList<String>();
						}
						relList.add(columns[5]);
						isarelationships.put(sourceId, relList);

						count++;
						if (count % 100000 == 0) {
							System.out.print(".");
						}
					}
					line = br.readLine();
				}
				logger.info(".");
				logger.info("Active isas Relationships for " +  isarelationships.size() + "  concepts loaded.");
			} finally {
				br.close();
			}
		}
	}
}
