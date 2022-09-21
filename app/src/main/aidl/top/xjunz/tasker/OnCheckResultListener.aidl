// OnCheckResultListener.aidl
package top.xjunz.tasker;

import top.xjunz.tasker.model.Result;

interface OnCheckResultListener {
   void onCheckResult(in Result result);
}