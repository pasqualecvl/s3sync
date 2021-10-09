package it.pasqualecavallo.s3sync.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileUtils {

	public static void createFileTree(String fullPath, byte[] content) throws IOException {
		String[] tokenized = tokenize(fullPath);
		if (tokenized.length > 1) {
			int index = 0;
			for (; index < tokenized.length - 1; index++) {
				Path toFolderPath = Paths.get(tokenized[index]);
				if (!Files.exists(toFolderPath)) {
					if (Files.isWritable(toFolderPath.getParent())) {
						Files.createDirectory(toFolderPath);
					} else {
						// FIXME: create custom exception
						throw new RuntimeException("Cannot create fs tree");
					}
				}
			}
		}
		try (FileOutputStream fos = new FileOutputStream(fullPath)) {
			  fos.write(content);
		}		
	}

	public static void deleteFileAndEmptyTree(String fullPath) {
		Path leafPath = Paths.get(fullPath);
		try {
			Files.delete(leafPath);
			do {
				if (leafPath.getParent().toFile().listFiles().length == 0) {
					Files.delete(leafPath.getParent());
					leafPath = leafPath.getParent();
				} else {
					break;
				}
			} while (!leafPath.getParent().toString().equals("/"));
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	private static String[] tokenize(String fullPath) {
		List<Integer> delimiterPosition = new ArrayList<>();
		for (int i = 0; i < fullPath.length(); i++) {
			if (fullPath.charAt(i) == '/') {
				if (fullPath.startsWith("/") && delimiterPosition.isEmpty()) {
					delimiterPosition.add(i);
				} else {
					delimiterPosition.add(i);
				}
			}
		}
		String[] tokens = new String[delimiterPosition.size() + 1];
		int i = 0;
		for (; i < delimiterPosition.size(); i++) {
			tokens[i] = fullPath.substring(0, delimiterPosition.get(i));
		}
		tokens[i] = fullPath;
		return tokens;
	}

	public static boolean notMatchFilters(List<Pattern> patterns, Path path) {
		if(patterns == null) {
			return true;
		}
		String pathName = path.toString();
		for(Pattern pattern : patterns) {
			if(pattern.matcher(pathName).matches()) {
				return false;
			}
		}
		return true;
	}
	
}
