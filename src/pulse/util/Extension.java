package pulse.util;

public enum Extension {
	
	/**
	 * The result will be an html-document with tags that can be opened with any web browser.
	 * Useful for complex formatting, but not for data manipulation, as it contains tags
	 * and special symbols.
	 */
	
	HTML, 
	
	/**
	 * The result will be a tab-delimited CSV document. Usefult for data manipulations and plotting.
	 */
	
	CSV;
	
	/**
	 * This will return the lower-case characters for the extension. 
	 */
	
	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
	
}