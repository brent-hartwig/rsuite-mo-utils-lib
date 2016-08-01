package com.rsicms.rsuite.utils.mo;

import java.io.IOException;

import com.rsicms.rsuite.utils.messsageProps.LibraryMessageProperties;

/**
 * Serves up formatted messages from messages.properties.
 */
public class MOUtilsMessageProperties extends LibraryMessageProperties {

  public MOUtilsMessageProperties() throws IOException {
    super(MOUtilsMessageProperties.class);
  }

}
