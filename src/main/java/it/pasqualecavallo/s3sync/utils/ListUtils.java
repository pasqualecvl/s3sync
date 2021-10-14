package it.pasqualecavallo.s3sync.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

	public static <T> List<T> getPageOf(List<T> list, Integer page, Integer pageSize) {
		List<T> sublist = null;
		int listSize = list != null ? list.size() : 0;
		if(listSize == 0) {
			sublist = new ArrayList<>();
		}
		if (listSize != 0 && page < (int) Math.ceil((double) listSize / (double) pageSize)) {
			if (listSize > ((page + 1) * pageSize)) {
				sublist = sublist.subList(pageSize * page, pageSize * (page + 1));
			} else {
				sublist = sublist.subList(pageSize * page, listSize);
			}
		}
		return sublist;
	}
	
}
