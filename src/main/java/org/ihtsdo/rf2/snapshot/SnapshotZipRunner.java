package org.ihtsdo.rf2.snapshot;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.utils.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class SnapshotZipRunner {

	private Object pathId;
	private String date;
	private File config;
	private Logger logger;
	private String defaultSnapshotFolder;
	private File tmpFolder;
	private File baseFolder;
	private String releaseDate;
	private String moduleId;
	private String namespace;
	private File exportSnapFolder;
	private File prevSnapFolder;
	private File zipPrevSnapshot;
	private File zipExportedDelta;
	private File tmpSortingFolder;


	public SnapshotZipRunner(File baseFolder, String releaseDate, String defaultSnapshotFolder, String moduleId, String namespace) throws ConfigurationException {
		this.baseFolder=baseFolder;
		this.config=config;
		this.date="99999999";
		this.pathId=pathId;
		this.releaseDate=releaseDate;
		this.moduleId=moduleId;
		this.namespace=namespace;
		this.defaultSnapshotFolder=defaultSnapshotFolder;
		cleanFolder();
		logger = Logger.getLogger("org.ihtsdo.rf2.snapshot.SnapshotZipRunner");
	}
	private void cleanFolder() {

		tmpFolder=new File( baseFolder, I_Constants.CLASSIFIER_EXPORT_TMP_FOLDER);
		if (!tmpFolder.exists()){
			tmpFolder.mkdirs();
		}else{
			FileHelper.emptyFolder(tmpFolder);
		}

		tmpSortingFolder=new File( baseFolder, I_Constants.CLASSIFIER_TEMP_SORTING_FOLDER);
		if (!tmpSortingFolder.exists()){
			tmpSortingFolder.mkdirs();
		}
		prevSnapFolder =new File( baseFolder , I_Constants.CLASSIFIER_PREVIOUS_SNAPSHOT_FOLDER);
		if (!prevSnapFolder.exists()){
			prevSnapFolder.mkdirs();
		}else{
			FileHelper.emptyFolder(prevSnapFolder);
		}
		exportSnapFolder=new File( baseFolder , I_Constants.SNAPSHOT_EXPORT_FOLDER);
		if (!exportSnapFolder.exists()){
			exportSnapFolder.mkdirs();
		}else{
			FileHelper.emptyFolder(exportSnapFolder);
		}
		zipPrevSnapshot=new File(baseFolder,I_Constants.CLASSIFIER_PREVIOUS_SNAP_ZIP_FILE);
		if (zipPrevSnapshot.exists()){
			zipPrevSnapshot.delete();
		}
		zipExportedDelta=new File(baseFolder,I_Constants.CLASSIFIER_EXPORTED_DELTA_ZIP_FILE);
		if (zipExportedDelta.exists()){
			zipExportedDelta.delete();
		}
	}
	public void execute() throws IOException, Exception{
		//origin folders
		File deltaExportFolder=new File( baseFolder, I_Constants.DELTA_EXPORT_FOLDER);
		File pathFolder=new File( baseFolder, I_Constants.CLASSIFIER_PREVIOUS_CLASSIFICATION_FILES_FOLDER);
		File defaultSnapshotFolderFile=new File(defaultSnapshotFolder);
		if (!pathFolder.exists()){
			pathFolder.mkdirs();
		}

		renameFilesForClassifier(deltaExportFolder,"Delta");
		String exportedOwlAxiom=FileHelper.getFile( deltaExportFolder, "rf2-owl-expression",null, "expression", "ontology");
//		String exportedOwlOntology=FileHelper.getFile( deltaExportFolder, "rf2-owl-ontology",null, "ontology", "axiom");

		exportedOwlAxiom=renameToDer2(exportedOwlAxiom);
//		exportedOwlOntology=renameToDer2(exportedOwlOntology);
		// zip for delta files
		FileHelper.pack(deltaExportFolder.getAbsolutePath(),zipExportedDelta.getAbsolutePath());

		// concepts
		String exportedConcepts=FileHelper.getFile( deltaExportFolder, "rf2-concepts", defaultSnapshotFolder ,null,null);

		String previousConcepts=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-concepts", pathFolder.getAbsolutePath(),null,null);

		// snapshot of previous concepts to zip previous
		File prevConceptsFile=new File(previousConcepts);
		File sortedConcepts=sortFile(prevConceptsFile);
		File concPrev=createSnapshot(new File(prevSnapFolder,I_Constants.CLASSIFIER_CONCEPT_FILENAME + "_Snapshot.txt"),sortedConcepts);
		if (prevConceptsFile.exists()){
			prevConceptsFile.delete();
		}
		FileHelper.copyTo(concPrev,prevConceptsFile);

		// snapshot of delta + previous concepts to clean inferred rels
		File outConcepts=getOutputFileFromMerge(exportedConcepts,prevConceptsFile.getAbsolutePath());
		sortedConcepts=sortFile(outConcepts);
		concPrev=createSnapshot(new File(exportSnapFolder,sortedConcepts.getName().substring(7)), sortedConcepts);
//
//		descriptions
		String exportedDescriptions=FileHelper.getFile( deltaExportFolder, "rf2-descriptions", defaultSnapshotFolder ,null,null);
		String previousDescriptions=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-descriptions", defaultSnapshotFolder,null,"definition");
		File outDescriptions=getOutputFileFromMerge(exportedDescriptions,previousDescriptions);

		// snapshot of delta + previous descriptions
		File sortedDescriptions=sortFile(outDescriptions);
		createSnapshot(new File(exportSnapFolder,sortedDescriptions.getName().substring(7)), sortedDescriptions);

		// stated rels
//		String exportedStateRels=FileHelper.getFile( deltaExportFolder, "rf2-relationships",defaultSnapshotFolder,"stated",null);
		String previousStatedRels=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-relationships",defaultSnapshotFolder,"stated",null);

		// snapshot of previous stated to zip previous
		File previousStatedRelsSnp=new File(previousStatedRels);
		File sortedStatedRels=sortFile(previousStatedRelsSnp);
		File statedRelPrevSnapshot=createSnapshot(new File(prevSnapFolder,I_Constants.CLASSIFIER_STATED_RELATIONSHIPS_FILENAME + "_Snapshot.txt"),sortedStatedRels);
//		if (previousStatedRelsSnp.exists()){
//			previousStatedRelsSnp.delete();
//		}
//		FileHelper.copyTo(statedRelPrevSnapshot,previousStatedRelsSnp);

//		Inferred rels cleaner to zip previous
		String exportedInfRels=FileHelper.getFile( deltaExportFolder, "rf2-relationships",defaultSnapshotFolder, null, "stated");
		String previousInfRels=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-relationships",defaultSnapshotFolder, null,"stated");

		File outInfRels=getOutputInferredFileFromMerge(exportedInfRels,previousInfRels);

		File sortedInfRels=sortFile(outInfRels);
		if (namespace!=null && !namespace.equals("0")){
			MovedComponents.retiredPreviousInferred(concPrev,sortedInfRels, tmpFolder, releaseDate,moduleId, namespace);
		}
		RetireInferrdPreviousOfRetiredConcept(prevConceptsFile,sortedInfRels, tmpFolder);
		File infPrev=createSnapshot(new File(prevSnapFolder,I_Constants.CLASSIFIER_INFERRED_RELATIONSHIPS_FILENAME + "_Snapshot.txt"),sortedInfRels);

		// mrcm files

		String exportedMrcmAttrDomain=FileHelper.getFile( deltaExportFolder, "rf2-mrcm-attr-domain",null, null, null);
		String exportedMrcmAttrRange=FileHelper.getFile( deltaExportFolder, "rf2-mrcm-attr-range",null, null, null);
		String exportedMrcmScope=FileHelper.getFile( deltaExportFolder, "rf2-mrcm-module-scope",null, null, null);
		String exportedMrcmDomain=FileHelper.getFile( deltaExportFolder, "rf2-mrcm-domain",null, null, null);

		String previousMrcmAttrDomain=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-mrcm-attr-domain",pathFolder.getAbsolutePath(), null, null);
		String previousMrcmAttrRange=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-mrcm-attr-range",pathFolder.getAbsolutePath(), null, null);
		String previousMrcmScope=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-mrcm-module-scope",pathFolder.getAbsolutePath(), null, null);
		String previousMrcmDomain=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-mrcm-domain",pathFolder.getAbsolutePath(), null, null);
		String previousOwlAxiom=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-owl-expression",pathFolder.getAbsolutePath(), "expression", "ontology");
//		String previousOwlOntology=FileHelper.getFile( defaultSnapshotFolderFile, "rf2-owl-ontology",pathFolder.getAbsolutePath(), "ontology", "axiom");

		//create snapshot for axioms in order to cycle checking in the next process
		File outAxioms=getOutputFileFromMerge(exportedOwlAxiom,previousOwlAxiom);

		// snapshot of delta + previous descriptions
		File sortedAxioms=sortFile(outAxioms);
		createSnapshot(new File(exportSnapFolder,sortedAxioms.getName()), sortedAxioms);


		// fix name for owl-classifier -it doesn't find if owl files start with sct2
        previousOwlAxiom=renameToDer2(previousOwlAxiom);
//        previousOwlOntology=renameToDer2(previousOwlOntology);

		// get Module and create snapshot from it
		Set<String> mrcmRefsetsId =null;
		if (exportedMrcmScope!=null) {
			mrcmRefsetsId = getMrcmRefsets(exportedMrcmScope);
		}else if (previousMrcmScope!=null) {
			mrcmRefsetsId = getMrcmRefsets(previousMrcmScope);
		}
		logger.info("MRCM Refsets:" + mrcmRefsetsId);
//		getSnapshot(mrcmRefsetsId, prevSnapFolder, previousMrcmScope);

		if (previousMrcmScope!=null){
			FileHelper.copyTo(new File(previousMrcmScope),new File(prevSnapFolder,new File(previousMrcmScope).getName()));
			FileHelper.copyTo(new File(previousMrcmAttrRange),new File(prevSnapFolder,new File(previousMrcmAttrRange).getName()));
			FileHelper.copyTo(new File(previousMrcmAttrDomain),new File(prevSnapFolder,new File(previousMrcmAttrDomain).getName()));
			FileHelper.copyTo(new File(previousMrcmDomain),new File(prevSnapFolder,new File(previousMrcmDomain).getName()));
		}
		/* TODO snapshot by pair referencedComponentId - module */
//		getSnapshot(mrcmRefsetsId, prevSnapFolder, previousMrcmAttrRange);
//		getSnapshot(mrcmRefsetsId, prevSnapFolder, previousMrcmAttrDomain);
//		getSnapshot(mrcmRefsetsId, prevSnapFolder, previousMrcmDomain);


		if (previousOwlAxiom!=null){
			FileHelper.copyTo(new File(previousOwlAxiom),new File(prevSnapFolder,new File(previousOwlAxiom).getName()));
		}
//		if (previousOwlOntology!=null){
//			FileHelper.copyTo(new File(previousOwlOntology),new File(prevSnapFolder,new File(previousOwlOntology).getName()));
//		}
//		String axiomModule=null;
//		if (exportedOwlAxiom!=null) {
//			axiomModule = getModule(exportedOwlAxiom);
//		}else if (previousOwlAxiom!=null) {
//			axiomModule = getModule(previousOwlAxiom);
//		}
//		logger.info("Axiom Module:" + axiomModule);
//		if (previousOwlAxiom!=null) {
//			getSnapshot(axiomModule, prevSnapFolder, previousOwlAxiom);
//		}
//
//		String ontoModule=null;
//		if (exportedOwlOntology!=null) {
//			ontoModule = getModule(exportedOwlOntology);
//		}else if (previousOwlOntology!=null) {
//			ontoModule = getModule(previousOwlOntology);
//		}
//		logger.info("Ontology Module:" + axiomModule);
//		if (previousOwlOntology!=null) {
//			getSnapshot(ontoModule, prevSnapFolder, previousOwlOntology);
//		}


		renameFilesForClassifier(prevSnapFolder,"Snapshot");
		FileHelper.pack(prevSnapFolder.getAbsolutePath(), zipPrevSnapshot.getAbsolutePath());

		//
	}

	private void RetireInferrdPreviousOfRetiredConcept(File prevConceptsFile, File sortedInfRels, File tmpFolder) throws IOException {
		if (!prevConceptsFile.exists() || !sortedInfRels.exists()){
			return;
		}
		BufferedReader br = FileHelper.getReader(prevConceptsFile);
		br.readLine();
		String[] spl;
		String line;
		HashSet<String> tmpRetConcepts=new HashSet<String>();
		while((line=br.readLine())!=null){
			spl=line.split("\t",-1);
			if (spl[2].equals("0")){
				tmpRetConcepts.add(spl[0]);

			}
		}
		br.close();
		File newRemovedInferFile = new File(tmpFolder, I_Constants.REMOVED_INFERRED_FILE + "tmp");
		BufferedWriter bw = FileHelper.getWriter(newRemovedInferFile);

		br= FileHelper.getReader(sortedInfRels);
		bw.append(br.readLine());
		bw.append("\r\n");
		HashSet<String>relIdsRetired=new HashSet<>();
		while((line=br.readLine())!=null) {
			spl=line.split("\t",-1);
//			if (tmpRetConcepts.contains(spl[4]) ||
//					tmpRetConcepts.contains(spl[5]) ||
//					tmpRetConcepts.contains(spl[7])) {
				if (tmpRetConcepts.contains(spl[4]) && spl[2].equals("1")) {
					relIdsRetired.add(spl[0]);
					for (int i=0;i<spl.length;i++){
						if (i==2){
							bw.append("0");
						}else{
							if (i==1){
								bw.append(releaseDate);
							}else{
								bw.append(spl[i]);
							}
						}
						if (i==spl.length-1){
							bw.append("\r\n");
						}else{
							bw.append("\t");
						}
					}
				}
//			}
		}
		br.close();
		File remInferr = new File(tmpFolder, I_Constants.REMOVED_INFERRED_FILE);
		if (remInferr.exists()) {
			br = FileHelper.getReader(new File(tmpFolder, I_Constants.REMOVED_INFERRED_FILE));
			br.readLine();
			while ((line = br.readLine()) != null) {
				spl = line.split("\t", -1);
				if (!relIdsRetired.contains(spl[0])) {
					bw.append(line);
					bw.append("\r\n");
				}
			}
			br.close();
			remInferr.delete();
		}
		bw.close();
		newRemovedInferFile.renameTo(remInferr);

	}

	private void renameFilesForClassifier(File deltaExportFolder, String fileType) {
        for (File file:deltaExportFolder.listFiles()){
            if (!file.isDirectory() && !file.isHidden()){

                String filePattern=FileHelper.getFileTypeByHeader(file);

                if (filePattern.equals("rf2-concepts")){
                    file.renameTo(new File(file.getParent(),I_Constants.CLASSIFIER_CONCEPT_FILENAME + "_" + fileType + "_.txt"));
                }else if (filePattern.equals("rf2-descriptions")){
                    file.renameTo(new File(file.getParent(),I_Constants.CLASSIFIER_DESCRIPTION_FILENAME + "_" + fileType + "_.txt"));
                }else if (filePattern.equals("rf2-relationships") && file.getName().toLowerCase().contains("_stated")){
                    file.renameTo(new File(file.getParent(),I_Constants.CLASSIFIER_STATED_RELATIONSHIPS_FILENAME + "_" + fileType + "_.txt"));
                }else if (filePattern.equals("rf2-relationships") && !file.getName().toLowerCase().contains("_stated")){
                    file.renameTo(new File(file.getParent(),I_Constants.CLASSIFIER_INFERRED_RELATIONSHIPS_FILENAME + "_" + fileType + "_.txt"));
                }else if (filePattern.equals("rf2-textDefinition")){
                    file.renameTo(new File(file.getParent(),I_Constants.CLASSIFIER_TEXTDEFINITION_FILENAME + "_" + fileType + "_.txt"));
                }else if (!file.getName().contains(fileType + "_")){
                	if (file.getName().contains(fileType)) {
						file.renameTo(new File(file.getParent(), file.getName().replace(fileType, fileType + "_")));
					}else{
						file.renameTo(new File(file.getParent(), file.getName().replace(".txt", fileType + "_.txt")));
					}
				}
            }
        }
    }

    private String renameToDer2(String filePath) {
	    if (filePath!=null) {
            File file = new File(filePath);
            if (file.exists()) {
                if (file.getName().toLowerCase().startsWith("sct2")) {
                    File newFile = new File(file.getParentFile(), file.getName().replace("sct2", "der2"));
                    file.renameTo(newFile);
//                    logger.info("Renamed to " + newFile.getAbsolutePath());
                    return newFile.getAbsolutePath();
                }

//				logger.info("Owl file was not renamed, returning " + file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }
		logger.info("Owl file was not renamed returning null");
        return null;
    }

    private void getSnapshot(Set<String> module, File destineFolder, String previousFile) {
		File inputFile = new File(previousFile);
		if (inputFile.exists() && module != null) {
			String[] refsets = new String[module.size()];
			Integer[] filterPosition = new Integer[module.size()];
			for (int i = 0; i < module.size(); i++) {
				filterPosition[i] = 4;
			}

//			logger.info("getSnapshot method input file:" + inputFile.getAbsolutePath());
			File outputFile = new File(tmpFolder, inputFile.getName());
			FileFilterAndSorter ffs = new FileFilterAndSorter(inputFile, outputFile, tmpSortingFolder, new int[]{0}, filterPosition, module.toArray(refsets));
			ffs.execute();
			ffs = null;
			createSnapshot(new File(destineFolder, inputFile.getName()), outputFile);
		}
	}

	private Set<String> getMrcmRefsets(String exportedMrcmScope) throws IOException {
		Set<String> refsets=new HashSet<String>();

		BufferedReader br = FileHelper.getReader(exportedMrcmScope);
		br.readLine();
		String[] spl;
		String line;
		int moduleCount=0;
		int anotherModule=0;
		int coreModule=0;
		while((line=br.readLine())!=null){
			spl=line.split("\t",-1);
			if (spl[3].equals(moduleId) && spl[2].equals("1")){
				moduleCount++;
			}else if (spl[2].equals("1") && spl[3].equals(I_Constants.META_MODULE_ID)){
				coreModule++;
			}else if (spl[2].equals("1")){
				anotherModule++;
			}
//			else if (!spl[3].equals(I_Constants.META_MODULE_ID)){
//				if (refsets==null || refsets.equals(I_Constants.META_MODULE_ID)){
//					refsets=spl[3];
//				}
//			}else if (refsets==null){
//				refsets=spl[3];
//			}
		}
		br.close();
		br = FileHelper.getReader(exportedMrcmScope);
		br.readLine();

		while((line=br.readLine())!=null){
			spl=line.split("\t",-1);
			if (moduleCount>=3) {
				if (spl[3].equals(moduleId) && spl[2].equals("1")) {
					refsets.add(spl[6]);
				}
			}else if (anotherModule>=3 ){
				if (spl[2].equals("1") && !spl[3].equals(I_Constants.META_MODULE_ID)) {
					refsets.add(spl[6]);
				}
			}else if (coreModule>=3 ){
				if (spl[2].equals("1") && spl[3].equals(I_Constants.META_MODULE_ID)) {
					refsets.add(spl[6]);
				}
			}
		}
		br.close();
		return refsets;
	}

	private String getModule(String rf2File) throws IOException {
		String module=null;

		BufferedReader br = FileHelper.getReader(rf2File);
		br.readLine();
		String[] spl;
		String line;

		while((line=br.readLine())!=null){
			spl=line.split("\t",-1);
			if (spl[3].equals(moduleId)){
				module=moduleId;
			}else if (!spl[3].equals(I_Constants.META_MODULE_ID)){
				if (module==null || module.equals(I_Constants.META_MODULE_ID)){
					module=spl[3];
				}
			}else if (module==null){
				module=spl[3];
			}
		}
		br.close();
		return module;
	}
	private File createSnapshot(File outputFile,File sortedFile) {

		logger.info("createSnapshot method input file:" + sortedFile.getAbsolutePath());
		logger.info("createSnapshot method output file:" + outputFile.getAbsolutePath());
		SnapshotGenerator sng=new SnapshotGenerator(sortedFile, date, 0, 1,outputFile, null, null) ;
		sng.execute();
		sng=null;
		System.gc();
		return outputFile;

	}
	private File sortFile(File file) {

		File sortedFile=new File(tmpFolder,"Sorted_" + file.getName() );

		FileSorter fs=new FileSorter(file, sortedFile, tmpSortingFolder, new int[]{0,1});
		fs.execute();
		fs=null;
		System.gc();

		return sortedFile;
	}
	private File getOutputFileFromMergeRelsSpecific(String exportedFile,String previousFile) throws IOException{

		File ouf=new File(exportedFile);

		File outFile=new File(tmpFolder,ouf.getName());
		CommonUtils.getLastVersionRelsReleaseSpecific(exportedFile,previousFile,outFile,new int[]{0});

		return outFile;
	}
	private File getOutputFileFromMerge(String exportedFile,String previousFile) throws IOException{

		File ouf=new File(exportedFile);

		File outFile=new File(tmpFolder,ouf.getName());
		CommonUtils.getLastVersionFile(exportedFile,previousFile,outFile,new int[]{0});

		return outFile;
	}
	private File getOutputInferredFileFromMerge(String exportedFile,String previousFile) throws IOException{

		File ouf=new File(exportedFile);

		File outFile=new File(tmpFolder,ouf.getName().replace("Delta","Snapshot"));
//		CommonUtils.getLastVersionFile(exportedFile,previousFile,outFile,new int[]{0});

		HashSet<File>tmpFile=new HashSet<File>();
		tmpFile.add(new File(exportedFile));
		tmpFile.add((new File(previousFile)));
		CommonUtils.concatFile(tmpFile,outFile);

		return outFile;
	}
}
