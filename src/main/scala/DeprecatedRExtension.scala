package org.nlogo.extensions.simpler

import org.nlogo.api.{ PrimitiveManager }

class DeprecatedRExtension extends SimpleRExtension {
  override def load(manager: PrimitiveManager): Unit = {
    // load all of the existing SimpleR prims
    super.load(manager)

    // load deprecated versions of the old R extension prims, advising on new alternative
    // primManager.addPrimitive("put", new Put());
    // primManager.addPrimitive("putNamedList", new PutNamedList());
    // primManager.addPrimitive("putList", new PutList());
    // primManager.addPrimitive("putDataframe", new PutDataframe());
    // primManager.addPrimitive("putAgent", new PutAgent());
    // primManager.addPrimitive("putAgentDf", new PutAgentDataFrame());
    // primManager.addPrimitive("eval", new Eval());
    // primManager.addPrimitive("__evalDirect", new EvalDirect());
    // primManager.addPrimitive("get", new Get());
    // primManager.addPrimitive("gc", new GC());
    // primManager.addPrimitive("clear", new ClearWorkspace());
    // primManager.addPrimitive("clearLocal", new ClearLocalWorkspace());
    // primManager.addPrimitive("interactiveShell", new interactiveShell());
    // primManager.addPrimitive("setPlotDevice", new SetPlotDevice());
    // primManager.addPrimitive("stop", new Stop());
    // primManager.addPrimitive("r-home", new DebugPrim(new RPath()));
    // primManager.addPrimitive("jri-path", new DebugPrim(new JRIPath()));
  }
}
