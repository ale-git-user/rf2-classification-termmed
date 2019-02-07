package org.ihtsdo.classifier.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

public class MovedComponents {

	public static void retiredPreviousInferred(File concPrev, File infPrev, File tmpFolder, String releaseDate, String moduleId, String namespace) throws IOException {
		FileInputStream fis = new FileInputStream(concPrev);
		InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
		BufferedReader br = new BufferedReader(isr);

		double lines = 0;
		String line;
		String[] spl;
		HashMap<String, String> hashcpt = new HashMap<String,String>();
		while ((line= br.readLine()) != null) {	
			spl=line.split("\t",-1);
			if (spl[0].contains(namespace)){
				hashcpt.put(spl[0], spl[3]);
			}
		}
		br.close();
		
		File outputFile=new File(infPrev.getParent(),infPrev.getName() + "tmp");
		FileOutputStream fos = new FileOutputStream( outputFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		File outputRemovedPrevInferredFile=new File(tmpFolder,I_Constants.REMOVED_INFERRED_FILE);
		FileOutputStream fost = new FileOutputStream( outputRemovedPrevInferredFile);
		OutputStreamWriter oswt = new OutputStreamWriter(fost,"UTF-8");
		BufferedWriter bwt = new BufferedWriter(oswt);
		
		fis = new FileInputStream(infPrev);
		isr = new InputStreamReader(fis,"UTF-8");
		br = new BufferedReader(isr);
		
		String header = br.readLine();
		bw.append(header);
		bw.append("\r\n");
		bwt.append(header);
		bwt.append("\r\n");
		
		String mod;
		while ((line= br.readLine()) != null) {		
			spl=line.split("\t",-1);
			mod=hashcpt.get(spl[4]);
			if (mod!=null){
				if (mod.equals(spl[3]) || spl[2].equals("0") ){
					bw.append(line);
					bw.append("\r\n");
				}
				else{
					lines++;
					for (int i=0;i<spl.length;i++){
						if (i==2){
							bw.append("0");
							bwt.append("0");
						}else{
							bw.append(spl[i]);
							if (i==1){
								bwt.append(releaseDate);
							}else{
								bwt.append(spl[i]);
							}
						}
						if (i==spl.length-1){
							bw.append("\r\n");
							bwt.append("\r\n");
						}else{
							bw.append("\t");
							bwt.append("\t");
						}

					}
				}
			}else{

				bw.append(line);
				bw.append("\r\n");
			}
		}
		bw.close();
		br.close();
		
		fis = new FileInputStream(infPrev);
		isr = new InputStreamReader(fis,"UTF-8");
		br = new BufferedReader(isr);
		
		header = br.readLine();
		String idprev="";
		String modprev="";
		String statusprev="";
		String linePrev="";
		String[] splPrev;
		while ((line= br.readLine()) != null) {
			spl=line.split("\t",-1);
			if (spl[0].equals(idprev)
					&& spl[2].equals("0")
						&& modprev.equals(moduleId) 
							&& statusprev.equals("1") ){
				splPrev=linePrev.split("\t",-1);
				for (int i=0;i<splPrev.length;i++){
					if (i==2){
						bwt.append("0");
					}else{
						if (i==1){
							bwt.append(releaseDate);
						}else{
							bwt.append(splPrev[i]);
						}
					}
					if (i<splPrev.length-1){
						bwt.append("\t");
					}
				}
				bwt.append("\r\n");
			}
			idprev=spl[0];
			statusprev=spl[2];
			modprev=spl[3];
			linePrev=line;
		}
		
		bwt.close();
		br.close();
		
		System.out.println("#Previous Inferred lines inactivated debt to distinct module: " + lines);
		if (lines==0){
			outputRemovedPrevInferredFile.delete();
		}
		infPrev.delete();
		outputFile.renameTo(infPrev);
		
	}

}
