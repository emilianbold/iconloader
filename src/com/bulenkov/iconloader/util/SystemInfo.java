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

  public static boolean isJavaVersionAtLeast(String v) {
    return StringUtil.compareVersionNumbers(JAVA_RUNTIME_VERSION, v) >= 0;
  }

  private static boolean isOracleJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && vendor.toLowerCase().contains("oracle");
  }

  private static boolean isAppleJvm() {
    final String vendor = getJavaVmVendor();
    return vendor != null && vendor.toLowerCase().contains("apple");
  }

  public static String getJavaVmVendor() {
    return System.getProperty("java.vm.vendor");
  }

}
