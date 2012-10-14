package jrFileSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;

public class JrFileUtils {
	public static <T extends JrListing> List<T> transformListing(Class<T> c, Map<String, String> listing) {
		List<T> returnList = new ArrayList<T>(listing.size());
		try {
			for (Map.Entry<String, String> item : listing.entrySet()) {
				T newItem = c.newInstance();
				newItem.mKey = Integer.parseInt(item.getValue());
				newItem.mValue = item.getKey();
				returnList.add(newItem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnList;
	}
	public static class SortJrListAsync<T extends JrListing> extends AsyncTask<List<T>, Void, List<T>> {
	
		private int partition(List<T> list, int left, int right) {
			int i = left, j = right;
			T tmp;
			String pivot = stripArticles(list.get((left + right) / 2).mValue);
	
			while (i <= j) {
				while (stripArticles(list.get(i).mValue).compareTo(pivot) < 0)
					i++;
				while (stripArticles(list.get(j).mValue).compareTo(pivot) > 0)
					j--;
				if (i <= j) {
					tmp = list.get(i);
					list.set(i, list.get(j));
					list.set(j, tmp);
					i++;
					j--;
				}
			}
	
			return i;
		}
		
		private String stripArticles(String input) {
			String lowerCaseInput = input.toLowerCase(); 
			if (lowerCaseInput.startsWith("a "))
				return input.substring(2);
			if (lowerCaseInput.startsWith("an "))
				return input.substring(3);
			if (lowerCaseInput.startsWith("the "))
				return input.substring(4);
			return input;
		}
		
		private void quickSort(List<T> list) {
			if (list.size() > 0)
				quickSort(list, 0, list.size() - 1);
		}
		
		private void quickSort(List<T> list, int left, int right) {
			int index = partition(list, left, right);
		      if (left < index - 1)
		            quickSort(list, left, index - 1);
		      if (index < right)
		            quickSort(list, index, right);
		}

		@Override
		protected List<T> doInBackground(List<T>... params) {
			quickSort(params[0]);
			return params[0];
		}

	}
}
