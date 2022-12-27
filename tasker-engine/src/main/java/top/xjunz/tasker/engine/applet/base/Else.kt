/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/11/03
 */
class Else : Do() {

    // Once the previous result is success, do not execute this flow
    override var isAnd = false
}