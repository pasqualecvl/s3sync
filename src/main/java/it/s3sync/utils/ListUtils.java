package it.s3sync.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

	public static <T> List<T> getPageOf(List<T> list, Integer page, Integer pageSize) {
		if(page == null || page < 1) { // -> handling page number
			page = 1;
		}
		if(pageSize == null || pageSize < 1) {
			pageSize = 20; //use default pagesize
		}
		page = page  - 1; // -> move to zero based
		List<T> sublist = new ArrayList<>();
		int listSize = list != null ? list.size() : 0;
		if (listSize != 0 && page < (int) Math.ceil((double) listSize / (double) pageSize)) {
			if (listSize > ((page + 1) * pageSize)) {
				sublist = list.subList(pageSize * page, pageSize * (page + 1));
			} else {
				sublist = list.subList(pageSize * page, listSize);
			}
		}
		return sublist;
	}
	
}
