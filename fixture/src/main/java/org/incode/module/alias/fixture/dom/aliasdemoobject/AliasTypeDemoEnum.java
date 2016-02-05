/*
 *  Copyright 2014 Dan Haywood
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.incode.module.alias.fixture.dom.aliasdemoobject;

import org.incode.module.alias.dom.api.aliasable.AliasType;

public enum AliasTypeDemoEnum implements AliasType {

    // in UK and NL
    GENERAL_LEDGER,
    // in UK and NL
    DOCUMENT_MANAGEMENT,
    // in UK only
    PERSONNEL_SYSTEM
    ;

    @Override
    public String getId() {
        return name();
    }
}
