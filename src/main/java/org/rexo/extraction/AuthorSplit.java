package org.rexo.extraction;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class AuthorSplit implements Serializable {

	public AuthorSplit() {}
	
  public List convert(String authorStr) {
    ArrayList lastNames = lastName( authorStr );
    ArrayList nameList = new ArrayList();

    if (lastNames.size() == 0) {
      return nameList;
    }

    authorStr = cleanAuthorString2( authorStr );

    String currentLastName = (String)lastNames.get( 0 );
    int indexNextAuthor = 0;
    int indexFirstAuthor = authorStr.indexOf( currentLastName );

    String name = "";

    if (indexFirstAuthor == 0) {
      int oldPos = 0;
      for (int i = 0; i < lastNames.size(); i++) {
        String lastName = (String)lastNames.get( i );
        int indexCurrentAuthor = authorStr.indexOf( lastName, oldPos );
        oldPos = indexCurrentAuthor;

        if (i < lastNames.size() - 1) {
          String lastName_plus = (String)lastNames.get( i + 1 );
          indexNextAuthor = authorStr.indexOf( lastName_plus, indexCurrentAuthor + 1 );
          name = authorStr.substring( indexCurrentAuthor, indexNextAuthor - 1 );
        }
        else {
          name = authorStr.substring( indexCurrentAuthor );
        }
        nameList.add( trimNonword( name ) );
      }
    }
    else {
      int oldPos = 0;
      for (int i = 0; i < lastNames.size(); i++) {
        String lastName = (String)lastNames.get( i );
        int indexCurrentAuthor = authorStr.indexOf( lastName, oldPos );
        name =
        authorStr.substring( oldPos, Math.min( indexCurrentAuthor + lastName.length(), authorStr.length() - 1 ) );
        oldPos = indexCurrentAuthor + lastName.length() + 1;
        nameList.add( trimNonword( name ) );
      }
    }

    return nameList;
  }

  String trimNonword(String str) {
    str = str.replaceFirst( "^[\\W\\s]+", "" );
    str = str.replaceFirst( "[\\W\\s]+$", "" );
    return str;
  }


  String cleanAuthorString2(String str) {
    String ss = str;
    ss = ss.replaceAll( " and", " ," );
    ss = ss.replaceAll( "\\.$", "" );
    ss = ss.replaceAll( ";", "" );
    return ss;
  }


  String cleanAuthorString(String str) {
    String ss = str;
    ss = ss.replaceAll( " \\w\\.", "" );
    ss = ss.replaceAll( "\\s\\w\\s\\.", "" );
    ss = ss.replaceAll( "^\\w\\.", "" );
    ss = ss.replaceAll( "^\\w\\s\\.", "" );
    ss = ss.replaceAll( " and", " ," );
    ss = ss.replaceAll( " &", " ," );
    ss = ss.replaceAll( "\\.$", "" );
    ss = ss.replaceAll( ";", "" );

    return ss;
  }

  ArrayList lastName(String ss) {
    ss = cleanAuthorString( ss );
    ArrayList names = new ArrayList();
    String[] authors = ss.split( "," );
    String last_name;
    for (int i = 0; i < authors.length; i++) {
      String author = authors[i];
      author = author.replaceAll( "^\\s+|\\s+$", "" );
      String[] first_last_name = author.split( " " );
      if (first_last_name.length == 2) {
        last_name = first_last_name[1];
      }
      else if (first_last_name.length == 1) {
        last_name = first_last_name[0];
      }
      else {
        last_name = first_last_name[first_last_name.length - 1];
      }
      if (last_name.length() > 1) {
        names.add( last_name );
      }
    }

    return names;
  }


  ArrayList LastName2(String ss) {

    ArrayList names = new ArrayList();

    //determine the type
    //there are four types of name conventions
    // 0: ,.     like helzerman , r . a . , and harper , m . p .
    // 1: .,     like v . dumortier , g . janssens , and m . bruynooghe .
    // 2: only . like p . prosser .
    // 3: only , like roberto bagnara, roberto giacobazzi, giorgio levi
    // 4: no punctuations like Fuchun Peng and Dale Schuurmans

    int indexPeriod = ss.indexOf( "." );
    int indexComma = ss.indexOf( "," );
    int type = -1;

    if (indexPeriod == -1 && indexComma == -1) {//no punctuations
      type = 4;
    }
    else if (indexPeriod == -1) {// only ,
      type = 3;
    }
    else if (indexComma == -1) {// only .
      type = 2;
    }
    else if (indexPeriod < indexComma) {//.,
      type = 1;
    }
    else if (indexComma < indexPeriod) {//,.
      type = 0;
    }
    else {
      throw new UnsupportedOperationException();
    }

    System.out.println( "	type=" + type );

    switch (type) {
      case 0:
        // in this case, last names are the word before ,
        while (true) {
          indexComma = ss.indexOf( "," );
          if (indexComma == -1) {
            break;
          }

          int indexBlank = ss.lastIndexOf( " ", indexComma - 2 );

          System.out.println( indexComma + "/" + indexBlank );

          String name = ss.substring( indexBlank + 1, indexComma - 1 );
          System.out.println( "lastname = \"" + name + "\"" );

          ss = ss.substring( indexComma + 1 );
        }
      case 1:
      case 2:
      case 3:
      case 4:
    }


    return names;
  }


}
