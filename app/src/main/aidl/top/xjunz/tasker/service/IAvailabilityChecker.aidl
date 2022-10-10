// IAvailabilityChecker.aidl
package top.xjunz.tasker.service;
import top.xjunz.tasker.service.IAvailabilityCheckerCallback;

interface IAvailabilityChecker {

    void launchCheck(int caseName, in IAvailabilityCheckerCallback listener);

}