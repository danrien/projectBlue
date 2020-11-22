package com.namehillsoftware.projectblue.shared;


public class XmlParsingHelpers {

	public static void HandleBadXml(StringBuilder currentSb, char[] ch, int start, int length) {
		currentSb.append(ch, start, length);
	}
}
