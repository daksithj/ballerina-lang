/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.nativeimpl.jvm.interop;

/**
 * This class represents constraint for a single Java method parameter and this is used in resolving a
 * Java method for Ballerina external functions.
 *
 * @since 1.0.0
 */
class ParamTypeConstraint {

    static final ParamTypeConstraint NO_CONSTRAINT = new ParamTypeConstraint(null);

    private Class<?> constraint;

    ParamTypeConstraint(Class<?> constraint) {
        this.constraint = constraint;
    }

    Class<?> get() {
        return this.constraint;
    }
}
