// IAvailabilityChecker.aidl
package top.xjunz.tasker.impl;
import top.xjunz.tasker.impl.IAvailabilityCheckerCallback;

interface IAvailabilityChecker {

    void launchCheck(int caseName, in IAvailabilityCheckerCallback listener);

}