package it.pasqualecavallo.s3sync.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

	public static void createFileTree(String fullPath) throws IOException {
		String[] tokenized = tokenize(fullPath);
		if(tokenized.length > 2) {
			int index = 0;
			for(;index < tokenized.length - 1; index++) {
				Path toFolderPath = Paths.get(tokenized[index]);
				if(!Files.exists(toFolderPath)) {
					if(Files.isWritable(toFolderPath.getParent())) {
						Files.createDirectory(toFolderPath);					
					} else {
						//FIXME: create custom exception
						throw new RuntimeException("Cannot create fs tree");
					}
				}
			}			
		}
	}
	public static void deleteFileAndEmptyTree(String fullPath) {
		Path leafPath = Paths.get(fullPath);
		do {
			
		} while(leafPath.normalize().getParent())

	}
	private static String[] tokenize(String fullPath) {
		List<Integer> delimiterPosition = new ArrayList<>();
		for(int i = 0; i < fullPath.length(); i++) {    
            if(fullPath.charAt(i) == '/') {
            	if(fullPath.startsWith("/") && delimiterPosition.isEmpty()) {
                	System.out.println("Found / at " + i);            		
                    delimiterPosition.add(i);            		
            	} else {
                	System.out.println("Found / at " + i + " with substring " + fullPath.substring(0, i));
                    delimiterPosition.add(i);            		
            	}
            }
        }
		
		String[] tokens = new String[ delimiterPosition.size() + 1];
		int i = 0;
		for(; i < delimiterPosition.size(); i++) {
			tokens[i] = fullPath.substring(0, delimiterPosition.get(i));
		}
		tokens[i] = fullPath;
		return tokens;
	}

}
