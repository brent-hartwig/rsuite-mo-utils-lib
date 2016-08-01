package com.rsicms.rsuite.utils.mo.qualifiers;

import javax.xml.namespace.QName;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.rsicms.rsuite.utils.mo.MOUtils;

/**
 * An managed object qualifier that accepts XML MOs with a specified QName.
 */
public class QNameManagedObjectQualifier implements ManagedObjectQualifier {

  private QName qname;
  private MOUtils moUtils;

  public QNameManagedObjectQualifier(QName qname) {
    this.qname = qname;
    this.moUtils = new MOUtils();
  }

  @Override
  public boolean accept(ManagedObject mo) throws RSuiteException {
    return moUtils.hasMatchingQName(mo, qname);
  }

}
