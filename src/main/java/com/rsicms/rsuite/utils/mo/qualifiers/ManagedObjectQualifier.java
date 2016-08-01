package com.rsicms.rsuite.utils.mo.qualifiers;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;

/**
 * Interface to qualify manage objects for an arbitrary operation.
 */
public interface ManagedObjectQualifier {

  /**
   * Determine if the provided MO meets criteria defined by the qualifier class.
   * 
   * @param mo
   * @return True if the MO is accepted by the qualifier.
   * @throws RSuiteException
   */
  public boolean accept(ManagedObject mo) throws RSuiteException;

}
