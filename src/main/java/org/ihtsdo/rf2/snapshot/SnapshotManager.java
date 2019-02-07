package org.ihtsdo.rf2.snapshot;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.ihtsdo.classifier.utils.CommonUtils;
import org.ihtsdo.classifier.utils.FileHelper;
import org.ihtsdo.classifier.utils.FileSorter;
import org.ihtsdo.classifier.utils.I_Constants;
import org.ihtsdo.classifier.utils.MovedComponents;


public class SnapshotManager {

	private String classifierFolder;
	private Object pathId;
	private String date;
	private File config;
	private Logger logger;
	private String dbName;
	private String defaultSnapshotFolder;
	private File tmpFolder;
	private String releaseDate;
	private String moduleId;
	private String namespace;


	public SnapshotManager(File config, String dbName, String pathId,String releaseDate, String defaultSnapshotFolder, String moduleId, String namespace) throws ConfigurationException {
		this.config=config;
		this.date="99999999";
		this.pathId=pathId;
		this.releaseDate=releaseDate;
		this.dbName=dbName;
		this.moduleId=moduleId;
		this.namespace=namespace;
		this.defaultSnapshotFolder=defaultSnapshotFolder;
		getParams();
		cleanFolder();
		logger = Logger.getLogger("org.ihtsdo.rf2.snapshot.SnapshotManager");
	}
	private void cleanFolder() {

		tmpFolder=new File( classifierFolder + "/" + dbName + "/" + pathId.toString() + "/temp");
		if (!tmpFolder.exists()){
			tmpFolder.mkdirs();
		}else{
			FileHelper.emptyFolder(tmpFolder);
		}
	}
	public void execute() throws IOException, Exception{
		File exportFolder=new File( classifierFolder + "/" + dbName + "/" + pathId.toString() + "/exported-delta");
		String exportedConcepts=FileHelper.getFile( exportFolder, "rf2-concepts", defaultSnapshotFolder ,null,null);

		String exportedDescriptions=FileHelper.getFile( exportFolder, "rf2-descriptions", defaultSnapshotFolder ,null,null);

		String exportedRels=FileHelper.getFile( exportFolder, "rf2-relationships",defaultSnapshotFolder,"stated",null);
		
		String exportedInfRels=FileHelper.getFile( exportFolder, "rf2-relationships",defaultSnapshotFolder, null, "stated");

		File pathFolder=new File( classifierFolder + "/" + dbName + "/" + pathId.toString() + "/previousFiles");
		if (!pathFolder.exists()){
			pathFolder.mkdirs();
		}
		File relFolder=new File(classifierFolder + "/releasedFiles/" + dbName);
		if (!relFolder.exists()){
			relFolder.mkdirs();
		}
		String previousConcepts=FileHelper.getFile( pathFolder, "rf2-concepts", defaultSnapshotFolder,null,null);
		File outConcepts=getOutputFileFromMerge(exportedConcepts,previousConcepts);

		String previousDescriptions=FileHelper.getFile( pathFolder, "rf2-descriptions", defaultSnapshotFolder,null,null);
		File outDescriptions=getOutputFileFromMerge(exportedDescriptions,previousDescriptions); 
		
		String previousRels=FileHelper.getFile( pathFolder, "rf2-relationships",defaultSnapshotFolder,"stated",null);
		
		File outRels;
//		if (dbName.equals("nl-edition")){
//			outRels=getOutputFileFromMergeRelsSpecific(exportedRels,previousRels);
//		}else{
			outRels=getOutputFileFromMerge(exportedRels,previousRels);
//		}
		String previousInfRels=FileHelper.getFile( pathFolder, "rf2-relationships",defaultSnapshotFolder, null,"stated");
		File outInfRels=getOutputInferredFileFromMerge(exportedInfRels,previousInfRels);

		File sortedConcepts=sortFile(outConcepts);
		File sortedDescriptions=sortFile(outDescriptions);
		File sortedRels=sortFile(outRels);
		File sortedInfRels=sortFile(outInfRels);

		File concPrev=createSnapshot(sortedConcepts);
		createSnapshot(sortedDescriptions);
		createSnapshot(sortedRels);
		
		
		if (namespace!=null && !namespace.equals("0")){
			MovedComponents.retiredPreviousInferred(concPrev,sortedInfRels, tmpFolder, releaseDate,moduleId, namespace);
		}
		File infPrev=createSnapshot(sortedInfRels);		

	}
	private File createSnapshot(File sortedFile) {

		File pathFolder=new File( classifierFolder + "/" + dbName + "/" + pathId.toString() + "/exported-snapshot");
		if (!pathFolder.exists()){
			pathFolder.mkdirs();
		}
		File outputFile=new File(pathFolder, sortedFile.getName().substring(7));
		SnapshotGenerator sng=new SnapshotGenerator(sortedFile, date, 0, 1,outputFile, null, null) ;
		sng.execute();
		sng=null;
		System.gc();
		return outputFile;

	}
	private File sortFile(File file) {
		
		File tmpSortingFolder=new File( classifierFolder + "/" + dbName + "/" + pathId.toString() + "/temp_sort");
		if (!tmpSortingFolder.exists()){
			tmpSortingFolder.mkdirs();
		}
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

		File outFile=new File(tmpFolder,ouf.getName());
//		CommonUtils.getLastVersionFile(exportedFile,previousFile,outFile,new int[]{0});

		HashSet<File>tmpFile=new HashSet<File>();
		tmpFile.add(new File(exportedFile));
		tmpFile.add((new File(previousFile)));
		CommonUtils.concatFile(tmpFile,outFile);

		return outFile;
	}
	private void getParams() throws ConfigurationException  {
		XMLConfiguration xmlConfig;
		try {
			xmlConfig=new XMLConfiguration(config);
		} catch (ConfigurationException e) {
			logger.info("ClassificationRunner - Error happened getting params file." + e.getMessage());
			throw e;
		}

		this.classifierFolder=xmlConfig.getString(I_Constants.CLASSIFIERFOLDER);
		xmlConfig=null;
	}	
}
