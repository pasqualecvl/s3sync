package it.s3sync.web.dto.response;

import java.util.ArrayList;
import java.util.List;

public class Folder {

	private String name = "/";
	private List<Folder> subfolder;
	private List<String> files;

	public List<Folder> getSubfolder() {
		return subfolder;
	}

	public void setSubfolder(List<Folder> subfolder) {
		this.subfolder = subfolder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}


	public void addFile(String name) {
		if(this.files != null) {
			this.files.add(name);
		} else {
			this.files = new ArrayList<>();
			this.files.add(name);
		}
	}
	
	public void addSubFolder(Folder subfolder) {
		if(this.subfolder != null) {
			this.subfolder.add(subfolder);
		} else {
			this.subfolder = new ArrayList<Folder>();
			this.subfolder.add(subfolder);
		}		
	}
	public Folder addNodes(String[] tokenized) {
		Folder latest = this;
		for(int i = 1; i < tokenized.length - 1; i++ ) {
			List<Folder> subfolders = getRigthDeepLevelSubfolders(i);

			boolean found = false;
			for(Folder f : subfolders) {
				if(tokenized[i].equals(f.getName())) {
					latest = f;
					found = true;
				} 
			}
			if(!found) {
				Folder folder = new Folder();
				folder.setName(tokenized[i]);
				latest.addSubFolder(folder);
				latest = folder;
			}
		}
		return latest;
	}

	
	private List<Folder> getRigthDeepLevelSubfolders(int i) {
		List<Folder> folders = this.getSubfolder();
		if(folders == null || folders.isEmpty()) {
			return new ArrayList<>();
		}
		List<Folder> tmp = new ArrayList<>();
  		for(int j = 1; j < i; j++) {
  			for(Folder f : folders) {
				tmp.add(f);  				
  			}
  		}
		folders = tmp;
  		return folders;
	}
}
