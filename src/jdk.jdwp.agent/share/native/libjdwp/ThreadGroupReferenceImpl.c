/*
 * Copyright (c) 1998, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "util.h"
#include "ThreadGroupReferenceImpl.h"
#include "threadControl.h"
#include "inStream.h"
#include "outStream.h"

static jboolean
name(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthreadGroup group;

    env = getEnv();

    group = inStream_readThreadGroupRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiThreadGroupInfo info;

        (void)memset(&info, 0, sizeof(info));
        threadGroupInfo(group, &info);
        (void)outStream_writeString(out, info.name == NULL ? "" : info.name);
        if ( info.name != NULL )
            jvmtiDeallocate(info.name);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
parent(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthreadGroup group;

    env = getEnv();

    group = inStream_readThreadGroupRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiThreadGroupInfo info;

        (void)memset(&info, 0, sizeof(info));
        threadGroupInfo(group, &info);
        (void)outStream_writeObjectRef(env, out, info.parent);
        if ( info.name != NULL )
            jvmtiDeallocate(info.name);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

/* vthread fixme: vthread support needs to be removed. */
static jboolean
children(PacketInputStream *in, PacketOutputStream *out)
{
     JNIEnv *env;
     jthreadGroup group;
     jboolean is_vthread_group;

     env = getEnv();

     group = inStream_readThreadGroupRef(env, in);
     if (inStream_error(in)) {
         return JNI_TRUE;
     }

     is_vthread_group = isSameObject(env, group, gdata->vthreadThreadGroup);

     WITH_LOCAL_REFS(env, 2) {

         jvmtiError error = JVMTI_ERROR_NONE;
         jint threadCount;
         jint vthreadCount = 0;
         jint groupCount;
         jthread *theThreads;
         jthread *theVThreads = NULL;
         jthread *theGroups;

         if (is_vthread_group) {
             if (gdata->enumerateVThreads) {
                 /* Get all the VThreads so we can return them. */
                 theVThreads = threadControl_allVThreads(&vthreadCount);
                 if (theVThreads == NULL && vthreadCount != 0) {
                     error = JVMTI_ERROR_OUT_OF_MEMORY;
                 }
             }
         }

         if (error == JVMTI_ERROR_NONE) {
             /* Get all the threads in this group so we can return them. */
             error = JVMTI_FUNC_PTR(gdata->jvmti,GetThreadGroupChildren)(gdata->jvmti, group,
                                              &threadCount,&theThreads,
                                              &groupCount, &theGroups);
         }

         if (error != JVMTI_ERROR_NONE) {
             outStream_setError(out, map2jdwpError(error));
         } else {
             int i;

             /* Squish out all of the debugger-spawned threads */
             threadCount = filterDebugThreads(theThreads, threadCount);

             (void)outStream_writeInt(out, threadCount + vthreadCount);
             for (i = 0; i < threadCount; i++) {
                 (void)outStream_writeObjectRef(env, out, theThreads[i]);
             }
             for (i = 0; i < vthreadCount; i++) {
                 (void)outStream_writeObjectRef(env, out, theVThreads[i]);
             }
             (void)outStream_writeInt(out, groupCount);
             for (i = 0; i < groupCount; i++) {
                 (void)outStream_writeObjectRef(env, out, theGroups[i]);
             }

             jvmtiDeallocate(theGroups);
             jvmtiDeallocate(theThreads);
             jvmtiDeallocate(theVThreads);
         }

     } END_WITH_LOCAL_REFS(env);

     return JNI_TRUE;
}

Command ThreadGroupReference_Commands[] = {
    {name, "Name"},
    {parent, "Parent"},
    {children, "Children"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ThreadGroupReference)
