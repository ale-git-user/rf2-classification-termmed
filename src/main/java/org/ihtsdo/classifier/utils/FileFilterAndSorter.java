package org.ihtsdo.classifier.utils;

import java.io.*;
import java.util.*;


public class FileFilterAndSorter {

	private File inputFile;
	private File outputFile;
	private File tempFolder;
	private int[] sortColumns;
	private final Integer SPLIT_SIZE = 150000;
	private Integer[] columnFilterIxs;
	private String[] columnFilterValues;

	public void execute() {
		try {
			FileHelper.emptyFolder(tempFolder);

			long start1 = System.currentTimeMillis();
			//int ln = countLines(file);

			FileInputStream fis = new FileInputStream(inputFile);
			InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
			BufferedReader br = new BufferedReader(isr);

			double lines = 1;
			List<String[]> list = new ArrayList<String[]>();
			String[] splittedLine;
			String nextLine;
			String header=br.readLine();
			boolean bContinue=true;
			while ((nextLine = br.readLine()) != null) {
//				nextLine = new String(nextLine.getBytes(),"UTF-8");
				splittedLine=nextLine.split("\t",-1);

				if (columnFilterIxs!=null){
					bContinue = false;
					for (int i=0;i<columnFilterIxs.length;i++){
						if (splittedLine[columnFilterIxs[i]].compareTo(columnFilterValues[i])==0){
							bContinue=true;
							break;
						}
					}
				}
				if (bContinue){
					list.add(splittedLine);
					if (lines % SPLIT_SIZE == 0) {
						FileOutputStream fos = new FileOutputStream(new File(tempFolder, "fileno" + lines / SPLIT_SIZE + ".txt"));
						OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
						writeFile(list, osw, sortColumns);
						list = new ArrayList<String[]>();
					}
					lines++;
				}
			}
			if (lines % SPLIT_SIZE != 0) {
				FileOutputStream fos = new FileOutputStream(new File(tempFolder, "fileno" + lines / SPLIT_SIZE + ".txt"));
				OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
				writeFile(list, osw, sortColumns);
			}
			br.close();
			sortFile(outputFile, tempFolder, header);
			long end1 = System.currentTimeMillis();
			long elapsed1 = (end1 - start1);
			System.out.println("Lines in output file  : " + lines);
			System.out.println("Completed in " + elapsed1 + " ms");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public FileFilterAndSorter(File inputFile, File outputFile, File tempFolder,
                               int[] sortColumns, Integer[] columnFilterIxs, String[] columnFilterValues) {
		super();
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.tempFolder = tempFolder;
		this.sortColumns = sortColumns;
		this.columnFilterIxs=columnFilterIxs;
		this.columnFilterValues=columnFilterValues;
	}


	public void writeFile(List<String[]> datos, OutputStreamWriter osw, int[] orderColumns) {
		try {
			BufferedWriter bw = new BufferedWriter(osw);
			Collections.sort(datos, new ArrayComparator(orderColumns, false));

			for (String[] row : datos) {
				for (int i = 0; i < row.length; i++) {
					bw.append(row[i]);
					if (i + 1 < row.length) {
						bw.append('\t');
					}
					
				}
				bw.append("\r\n");
			}
			bw.close();
			datos = null;
			System.gc();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public void sortFile( File outputFile, File tempFolder, String header) {
		try {

			if (outputFile.exists()){
				outputFile.delete();
			}
			FileOutputStream fos = new FileOutputStream(outputFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			BufferedWriter sortedBw = new BufferedWriter(osw);
			sortedBw.append(header);
			sortedBw.append("\r\n");
			// Merge
			File[] files = tempFolder.listFiles();

			HashMap<BufferedReader, String[]> readers = new HashMap<BufferedReader, String[]>();
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isHidden()) {
					FileInputStream fis = new FileInputStream(files[i]);
					InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
					BufferedReader bufReader = new BufferedReader(isr);
					String row;
					if ((row = bufReader.readLine()) != null) {
						readers.put(bufReader, row.split("\t",-1));
					} else {
						bufReader.close();
						files[i].delete();
						readers.remove(bufReader);
					}
				}
			}

			while (!readers.isEmpty()) {
				String[] smallestRow = CommonUtils.getSmallestArray(readers,sortColumns);
				for (int i = 0; i < smallestRow.length; i++) {
					sortedBw.append(smallestRow[i]);
					if (i + 1 < smallestRow.length) {
						sortedBw.append('\t');
					}
				}
				sortedBw.append("\r\n");

				Iterator<BufferedReader> it = readers.keySet().iterator();
				BufferedReader smallest = null;
				while (it.hasNext()) {
					BufferedReader bufferedReader = (BufferedReader) it.next();
					if (readers.get(bufferedReader).equals(smallestRow)) {
						smallest = bufferedReader;
						break;
					}
				}
				if (smallest.ready()) {
					readers.put(smallest, smallest.readLine().split("\t",-1));
				} else {
					smallest.close();
					readers.remove(smallest);
				}
			}

			sortedBw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
