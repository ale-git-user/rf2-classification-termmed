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

import com.google.gson.Gson;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.model.*;
import org.ihtsdo.classifier.utils.FileHelper;
import org.ihtsdo.classifier.utils.I_Constants;
import org.ihtsdo.json.model.ConceptDescriptor;
import org.ihtsdo.json.model.LightConceptDescriptor;
import org.ihtsdo.json.model.RelationshipVersion;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.*;
import java.util.*;

/**
 * The Class ClassificationRunner.
 * This class is responsible to classify stated relationships from RF2 format using snorocket reasoner.
 * Output results are inferred relationships composed of taxonomy and attributes.
 * Inferred relationships are saved in file which is a parameter of class constructor.
 *
 * @author Alejandro Rodriguez.
 *
 * @version 1.0
 */
public class PostClassificationRunner {

    private final File baseFolder;
    private final File resultFile;
	private final File reportfolder;

	/** The output tmp. */
	private File tempRelationshipDeltaStore;
	private String executionId;
	private Report report;

	private String defaultSnapshotFolder;
    private File tempEquivalentStore;
    private String previousInfRels;

    public PostClassificationRunner(File baseFolder, File resultFile,
									String moduleId, String date,
									String executionId,
									String defaultSnapshotFolder, String defaultLangCode, File reportFolder) throws IOException, Exception {
        this.baseFolder=baseFolder;
        this.resultFile=resultFile;
        this.module = moduleId;
		this.releaseDate=date;
		this.executionId=executionId;
		this.defaultSnapshotFolder=defaultSnapshotFolder;
		this.defaultLangCode=defaultLangCode;
		this.reportfolder=reportFolder;
		logger = Logger.getLogger("org.ihtsdo.classifier.PostClassificationRunner");


	}

	/** The logger. */
	private  Logger logger;

	/** The concept module. */
	private  HashMap<String,Concept> conceptModule;

	/** The module. */
	private  String module;

	/** The release date. */
	private  String releaseDate;

	/** The concepts. */
	private  String concepts;

	/** The output rels. */
	private  String newSnapInferredRelationships;

	/** The retired set. */
	private HashSet<String> retiredSet;

	private HashSet<String> modifiedConcepts;

	private String newDeltaInferredRelationships;

	private File reportFile;

	private String descriptions;

	private HashMap<String, String> defaultTerms;

	private String defaultLangCode;

	private HashSet<String> tmpConcepts;

	private File tmpFolder;

	private File removedPrevInferredFile;


	/**
	 * Execute the classification.
	 */
	public void execute(){

		try {
            modifiedConcepts=new HashSet<String>();
            retiredSet=new HashSet<String>();
            tmpConcepts=new HashSet<String>();
            conceptModule=new HashMap<String,Concept>();

		    String destFolder=resultFile.getAbsolutePath().replace(".zip","");
		    File destFolderFile=new File(destFolder);
		    if (!destFolderFile.exists()){
		        destFolderFile.mkdirs();
            }
			unzip(resultFile.getAbsolutePath(), destFolder);
			report=new Report();
			report.setExecutionId(executionId);

            File outFolder=new File(baseFolder, I_Constants.CLASSIFIER_POST_PROCESS_OUTPUT_FOLDER);
            if (!outFolder.exists()){
                outFolder.mkdirs();
            }
            this.reportFile=new File(reportfolder ,I_Constants.CLASSIFIER_REPORT_FILENAME);

            if (reportFile.exists()){
                reportFile.delete();
            }
            this.newSnapInferredRelationships=outFolder.getAbsolutePath() + "/sct2_Relationship_Snapshot.txt";
            this.newDeltaInferredRelationships=outFolder.getAbsolutePath() + "/sct2_Relationship_Delta.txt";

            File exportSnapFolder=new File( baseFolder , I_Constants.SNAPSHOT_EXPORT_FOLDER);
            concepts= FileHelper.getFile( exportSnapFolder, "rf2-concepts", defaultSnapshotFolder,null,null);

            descriptions=FileHelper.getFile( exportSnapFolder, "rf2-descriptions", defaultSnapshotFolder,null,"definition");

            tmpFolder=new File( baseFolder, I_Constants.CLASSIFIER_EXPORT_TMP_FOLDER);
            if (!tmpFolder.exists()){
                tmpFolder.mkdirs();
            }
            removedPrevInferredFile=new File(tmpFolder,I_Constants.REMOVED_INFERRED_FILE);

            File prevSnapFolder =new File( baseFolder , I_Constants.CLASSIFIER_PREVIOUS_SNAPSHOT_FOLDER);
            previousInfRels=FileHelper.getFile( prevSnapFolder, "rf2-relationships",defaultSnapshotFolder, null,"stated");

            AddDefaultTerms();
            getConceptModule();
            writeEquivConcept();
            consolidateRels();
            writeReport();

            defaultTerms=null;
            modifiedConcepts=null;
            retiredSet=null;
            tmpConcepts=null;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final int BUFFER_SIZE = 4096;
	/**
	 * Extracts a zip file specified by the zipFilePath to a directory specified by
	 * destDirectory (will be created if does not exists)
	 * @param zipFilePath
	 * @param destDirectory
	 * @throws IOException
	 */
	public void unzip(String zipFilePath, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			String filePath = destDirectory + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				extractFile(zipIn, filePath);
                if (entry.getName().toLowerCase().contains("relationship")){
                    tempRelationshipDeltaStore =new File(filePath);
                }else if (entry.getName().toLowerCase().contains("equivalent")){
                    tempEquivalentStore=new File(filePath);
                }
			} else {
				// if the entry is a directory, make the directory
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}
	/**
	 * Extracts a zip entry (file entry)
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
	    File file=new File(filePath);
	    File parentFile=file.getParentFile();
	    if (!parentFile.exists()){
	        parentFile.mkdirs();
        }
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}

	private void writeReport() throws IOException {

		BufferedWriter bwr = FileHelper.getWriter(reportFile);
		Gson gson=new Gson();
		bwr.append(gson.toJson(report));

		bwr.close();
		bwr=null;

	}

	private void writeEquivConcept() throws IOException {

        BufferedReader br = FileHelper.getReader(tempEquivalentStore);

        br.readLine();
        String line;
        String[] spl;
		HashMap<String, List<String>> hashtmp = new HashMap<String, List<String>>();
        List<ArrayList<ConceptDescriptor>> equivGroupList = new ArrayList<ArrayList<ConceptDescriptor>>();

        while((line=br.readLine())!=null) {
			spl = line.split("\t", -1);

			List<String> equivcompo = hashtmp.get(spl[6]);
			if (equivcompo == null) {
				equivcompo = new ArrayList<String>();
			}

			equivcompo.add(spl[5]);

			hashtmp.put(spl[6], equivcompo);
		}

		for (String key: hashtmp.keySet()){

			List<String> equivcompo =hashtmp.get(key);

            ArrayList<ConceptDescriptor> group= new ArrayList<ConceptDescriptor>();

            for (String component : equivcompo) {
				group.add(getConceptDescriptor(component));
			}
            equivGroupList.add(group);
        }
        report.setEquivalentConceptGroups(equivGroupList);
        if (equivGroupList.size()>0){
			report.setEquivalentsDefinitionsDetected("Yes");
        }else{
		    report.setEquivalentsDefinitionsDetected("No");
        }
	}

    private void getConceptModule() throws IOException {

        BufferedReader rbr = FileHelper.getReader(concepts);
        rbr.readLine();
        String line;
        String[] spl;
        while((line=rbr.readLine())!=null){
            spl=line.split("\t",-1);
            if (tmpConcepts.contains(spl[0])){
                conceptModule.put(spl[0], new Concept(0,spl[4].equals("900000000000074008")? false:true,spl[3]));
            }
        }
        rbr.close();
        rbr=null;
    }
	private void AddDefaultTerms() throws IOException {
		if (defaultTerms==null){
			defaultTerms=new HashMap<String,String>();
		}
		collectConceptIds(tempEquivalentStore,new int[]{5,6});
        collectConceptIds(tempRelationshipDeltaStore,new int[]{4,5,7});
        if (removedPrevInferredFile.exists()) {
            collectConceptIds(removedPrevInferredFile, new int[]{4, 5, 7});
        }
		BufferedReader rbr = FileHelper.getReader(descriptions);
		rbr.readLine();
		String line;
		String[] spl;
		while((line=rbr.readLine())!=null){
			spl=line.split("\t",-1);
			if (tmpConcepts.contains(spl[4])){
				if (spl[2].equals("1")){
					if (spl[6].equals("900000000000003001")){
						if (spl[5].equals(defaultLangCode)){
							defaultTerms.put(spl[4], spl[7]);
						}else if (!defaultTerms.containsKey(spl[4])){
							defaultTerms.put(spl[4], spl[7]);
						}
					}else if (!defaultTerms.containsKey(spl[4])){
						if (spl[5].equals(defaultLangCode)){
							defaultTerms.put(spl[4], spl[7]);
						}
					}
				}
			}
		}
		rbr.close();
		rbr=null;

	}

    private void collectConceptIds(File file, int[] ints) throws IOException {

        BufferedReader br = FileHelper.getReader(file);
        String line;
        String[] spl;
        br.readLine();
        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            for(int i:ints) {
                tmpConcepts.add(spl[i]);
            }

        }
        br.close();
    }

    private ConceptDescriptor getConceptDescriptor(String conceptId){

		ConceptDescriptor cd=new ConceptDescriptor();
		cd.setConceptId(conceptId);
        Concept concept=conceptModule.get(conceptId);
        String moduleC1=null;
        if (concept!=null){
            cd.setDefinitionStatus(concept.isDefined==true? "900000000000073002":"900000000000074008");
		    moduleC1=concept.getModule();
			if (moduleC1==null){
                cd.setModule(module);
			}
		}else{
            cd.setDefinitionStatus("900000000000074008");
            cd.setModule(module);
        }
		cd.setDefaultTerm(getDefaultTerm(conceptId));
		return cd;
	}

	private String getDefaultTerm(String conceptId) {
		if (defaultTerms!=null){
			return defaultTerms.get(conceptId);
		}
		return null;
	}

	/**
	 * Consolidate rels.
	 *
	 * @throws Exception the exception
	 */
	private void consolidateRels() throws IOException {

		BufferedWriter bw = FileHelper.getWriter(new File(newSnapInferredRelationships));

		BufferedWriter bwd = FileHelper.getWriter(new File(newDeltaInferredRelationships));

		BufferedReader rbr = FileHelper.getReader(tempRelationshipDeltaStore);
		String header=rbr.readLine();

		bw.append(header);
		bw.append("\r\n");
		bwd.append(header);
		bwd.append("\r\n");

		Changes changes=new Changes();
		changes.setNewInferredIsas(new ArrayList<RelationshipVersion>() );
		changes.setNewAttributes(new ArrayList<RelationshipVersion>() );
		changes.setLostInferredIsas(new ArrayList<RelationshipVersion>() );
		changes.setLostAttributes(new ArrayList<RelationshipVersion>() );

		String line;
		String[] spl;
		String id;
		while((line=rbr.readLine())!=null){
		    spl=line.split("\t",-1);
		    if (spl[0]==null || spl[0].trim().equals("")){
		        id=UUID.randomUUID().toString();
            }else{
		        id=spl[0];
		        retiredSet.add(id);
            }
            StringBuffer sb=new StringBuffer();
		    sb.append(id);
            sb.append("\t");
            sb.append(releaseDate);
            sb.append("\t");
            sb.append(spl[2]);
            sb.append("\t");
            sb.append(module);
            sb.append("\t");
            sb.append(spl[4]);
            sb.append("\t");
            sb.append(spl[5]);
            sb.append("\t");
            sb.append(spl[6]);
            sb.append("\t");
            sb.append(spl[7]);
            sb.append("\t");
            sb.append(spl[8]);
            sb.append("\t");
            sb.append(spl[9]);
            line=sb.toString();

			changes=addRelToReportChanges(line,changes);

			bw.append(line);
			bw.append("\r\n");
			bwd.append(line);
			bwd.append("\r\n");

            modifiedConcepts.add(spl[4]);
		}
		rbr.close();
		rbr=null;

		if (removedPrevInferredFile.exists()){
			rbr= FileHelper.getReader(removedPrevInferredFile);
			rbr.readLine();

			while((line=rbr.readLine())!=null){
				spl=line.split("\t",-1);
                retiredSet.add(spl[0]);
				modifiedConcepts.add(spl[4]);
				bw.append(line);
				bw.append("\r\n");
				bwd.append(line);
				bwd.append("\r\n");
			}
			rbr.close();
		}
		report.setConceptInCycles(new ArrayList<ConceptDescriptor>());
		report.setCyclesDetected("No");

		report.setChanges(changes);

        File previousInfRelsFile=new File(previousInfRels);
		if (previousInfRelsFile.exists()){

			rbr = FileHelper.getReader(previousInfRelsFile);
			rbr.readLine();
			while((line=rbr.readLine())!=null){

				spl=line.split("\t",-1);
				if (retiredSet.contains(spl[0])){
					continue;
				}
				if (spl[2].equals("0")){
					continue;
				}

                if (modifiedConcepts.contains(spl[4]) ){
                    bwd.append(line);
                    bwd.append("\r\n");
                }
                bw.append(line);
                bw.append("\r\n");
			}
			rbr.close();
			rbr=null;

		}

		bw.close();
		bwd.close();
	}

	private Changes addRelToReportChanges(String line, Changes changes) {
		String[] spl=line.split("\t",-1);

		RelationshipVersion r = new RelationshipVersion();
		r.setRelationshipId(spl[0]);
		r.setEffectiveTime(spl[1]);
		r.setActive(spl[2]);
		r.setModule(spl[3]);
		r.setGroupId(Integer.valueOf( spl[6]));
		r.setModifier(spl[8]);
		r.setSourceId(spl[4]);
		r.setTarget(getConceptDescriptor(spl[5]));
		r.setType(newLightConceptDescriptor(spl[7]));
		r.setCharType(newLightConceptDescriptor(spl[8]));
		r.setP("0");

		if (spl[2].equals("1")){
			if (spl[7].equals(I_Constants.ISA)){
				if (changes.getNewInferredIsas().size()<1000){
					changes.getNewInferredIsas().add(r);
				}
			}else{
				if (changes.getNewAttributes().size()<1000){
					changes.getNewAttributes().add(r);
				}
			}
		}else{
			if (spl[7].equals(I_Constants.ISA)){
				if (changes.getLostInferredIsas().size()<1000){
					changes.getLostInferredIsas().add(r);
				}
			}else{
				if (changes.getLostAttributes().size()<1000){
					changes.getLostAttributes().add(r);
				}
			}

		}
		return changes;
	}

	private LightConceptDescriptor newLightConceptDescriptor(String id) {
		LightConceptDescriptor lcd=new LightConceptDescriptor();
		lcd.setConceptId(id);
		lcd.setDefaultTerm(defaultTerms.get(id));
		return lcd;
	}

}
