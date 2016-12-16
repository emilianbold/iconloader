/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bulenkov.iconloader.util;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings({"HardCodedStringLiteral", "UtilityClassWithoutPrivateConstructor", "UnusedDeclaration"})
public class SystemInfo {
  public static final String OS_NAME = System.getProperty("os.name");
  public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");

  protected static final String _OS_NAME = OS_NAME.toLowerCase();
  public static final boolean isMac = _OS_NAME.startsWith("mac");

  public static final boolean isAppleJvm = isAppleJvm();
  public static final boolean isOracleJvm = isOracleJvm();

  public static int compareVersionNumbers(String v1, String v2) {
    if (v1 == null && v2 == null) {
      return 0;
    }
    if (v1 == null) {
      return -1;
    }
    if (v2 == null) {
      return 1;
    }

    String[] part1 = v1.split("[\\.\\_\\-]");
    String[] part2 = v2.split("[\\.\\_\\-]");

    int idx = 0;
    for (; idx < part1.length && idx < part2.length; idx++) {
      String p1 = part1[idx];
      String p2 = part2[idx];

      int cmp;
      if (p1.matches("\\d+") && p2.matches("\\d+")) {
        cmp = new Integer(p1).compareTo(new Integer(p2));
      } else {
        cmp = part1[idx].compareTo(part2[idx]);
      }
      if (cmp != 0) return cmp;
    }

    if (part1.length == part2.length) {
      return 0;
    } else if (part1.length > idx) {
      return 1;
    } else {
      return -1;
    }
  }

  public static boolean isJavaVersionAtLeast(String v) {
    return StringUtil.compareVersionNumbers(JAVA_RUNTIME_VERSION, v) >= 0;
  }

  private static boolean isOracleJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "Oracle");
  }

  private static boolean isAppleJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "Apple");
  }

  public static String getJavaVmVendor() {
    return System.getProperty("java.vm.vendor");
  }

}
