package it.s3sync.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
	public static List<String> createFileTree(String fullPath, byte[] content, Long lastModified) throws IOException {
		String[] tokenized = tokenize(fullPath);
		List<String> folders = new ArrayList<>();
		logger.trace("Tokenizing string: " + fullPath + " returns with " + Arrays.toString(tokenized));
		if (tokenized.length > 1) {
			int index = 0;
			for (; index < tokenized.length - 1; index++) {
				Path toFolderPath = Paths.get(tokenized[index]);
				if (!Files.exists(toFolderPath)) {
					if (Files.isWritable(toFolderPath.getParent())) {
						folders.add(toFolderPath.toString());
						Files.createDirectory(toFolderPath);
					} else {
						// FIXME: create custom exception
						throw new RuntimeException("Cannot create fs tree");
					}
				}
			}
		} else {
			logger.debug("Tokenization returns with one token, which is the file name. Write the content to /" + fullPath);
		}
		try (FileOutputStream fos = new FileOutputStream(fullPath)) {
			  fos.write(content);
		}
		Files.setLastModifiedTime(Path.of(fullPath), FileTime.from(lastModified, TimeUnit.MILLISECONDS));
		return folders;
	}

	public static List<String> toDeleteFileAndEmptyTree(String fullPath) {
		List<String> deleteFolders = new ArrayList<>();
		Path leafPath = Paths.get(fullPath);
		try {
			if(Files.exists(leafPath)) {
				Files.delete(leafPath);
				do {
					if (leafPath.getParent().toFile().listFiles().length == 0) {
						deleteFolders.add(leafPath.getParent().toString());
						leafPath = leafPath.getParent();
					} else {
						break;
					}
				} while (!leafPath.getParent().toString().equals("/"));
			}
		} catch (IOException e) {
			logger.error("Exception", e);
		}
		return deleteFolders;
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

	public static boolean notMatchFilters(List<Pattern> patterns, String path) {
		if(patterns == null) {
			return true;
		}
		for(Pattern pattern : patterns) {
			if(pattern.matcher(path).matches()) {
				return false;
			}
		}
		return true;
	}
	
}
