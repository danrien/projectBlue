package jrFileSystem;

import java.util.ArrayList;
import java.util.List;
import jrAccess.JrStdXmlResponse;
import jrAccess.JrSession;



public class JrFileSystem extends JrListing {
	private List<JrPage> mPages;
	
//	public jrFileSystem(int key, String value) {
//		super(key, value);
//		// TODO Auto-generated constructor stub
//		setPages();
//	}
	
//	public jrFileSystem(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setPages();
//	}
	
	public JrFileSystem() {
		super();
//		setPages();
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	public List<JrPage> getPages() {
		if (mPages == null) {
			mPages = new ArrayList<JrPage>();
			if (JrSession.accessDao == null) return mPages;

			try {
				mPages = JrFileUtils.transformListing(JrPage.class, (new JrStdXmlResponse()).execute("Browse/Children").get().items);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mPages;
	}
	
}

