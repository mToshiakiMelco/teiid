/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
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

package org.teiid.security;

import java.security.Principal;
import java.util.Enumeration;

/**
 * A {@link Principal} that represents a named group of member principals, e.g. a "Roles" group
 * whose members are the role names granted to a subject.
 * <p>
 * This is the Teiid replacement for {@code java.security.acl.Group}, which was removed from the JDK
 * in Java 14. A security integration that wants Teiid to extract roles from a {@link javax.security.auth.Subject}
 * should add a principal implementing this interface (named "Roles") to the subject's principals.
 */
public interface GroupPrincipal extends Principal {

    /** The member principals of this group (for a roles group, the individual role principals). */
    Enumeration<? extends Principal> members();
}
