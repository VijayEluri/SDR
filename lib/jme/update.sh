#!/bin/sh
# first build 'webdist-nativelibs' in dist/jme
JME_CVS=../../../Jme
cp ${JME_CVS}/target/*.jar ${JME_CVS}/lib/*.{so,jar,dll,jnilib,dylib} \
   ${JME_CVS}/lib/mvn-lib-install .
/bin/rm -f lwjgl_test.jar jmetest.jar jmetest-data.jar
mkdir -p jnlp
cp ${JME_CVS}/jnlp/*.jar jnlp
