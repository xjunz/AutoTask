// IAccessibilityServiceEnabler.aidl
package top.xjunz.tasker.service;

// Declare any non-default types here with import statements

interface IAccessibilityServiceEnabler {

      void enable(in String pkgName,in String clsName)=1;

      void destroy() = 16777114; // Destroy method defined by Shizuku server
}