package org.rexo.extraction;


public class SGMLStringOperation
{
	static String sgmlRegex = "</?([^>]*)>";

	public static String locateField(String startTag, String endTag, String string)
        {
                int indexStart = string.indexOf(startTag);
                int indexEnd   = string.indexOf(endTag, indexStart);
                                                                                                                                                             
                if(indexStart == -1 || indexEnd == -1){
                        return "";
                }
                else{
                        return string.substring(indexStart+startTag.length(), indexEnd-1);
                }
                                                                                                                                                             
        }
	
	public static String removeSGMLTags(String sgmlString)
	{
		sgmlString.replaceAll(sgmlRegex, "");
		
		return sgmlString;
	}

}
