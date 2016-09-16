package org.renjin.compiler.pipeline.fusion;

import org.renjin.compiler.pipeline.fusion.kernel.ColSumKernel;
import org.renjin.compiler.pipeline.fusion.kernel.LoopKernel;
import org.renjin.compiler.pipeline.fusion.kernel.RowMeanKernel;
import org.renjin.compiler.pipeline.fusion.kernel.SumMeanKernel;
import org.renjin.compiler.pipeline.node.ComputationNode;
import org.renjin.compiler.pipeline.node.DeferredNode;

import java.util.HashMap;
import java.util.Map;


public enum LoopKernels {

  INSTANCE;

  private Map<String, LoopKernel> map;

  LoopKernels() {
    map = new HashMap<>();
    if (System.getProperty("renjin.vp.disablejit") == null) {
      map.put("mean", SumMeanKernel.mean());
      map.put("sum", SumMeanKernel.sum());
      map.put("rowMeans", new RowMeanKernel());
      map.put("colSums", new ColSumKernel());
    } else {
      System.err.println("Specializers are disabled");
    }
  }

  public boolean supports(DeferredNode node) {
    if(node instanceof ComputationNode) {
      return map.containsKey(((ComputationNode) node).getComputationName());
    }
    return false;
  }

  public LoopKernel get(ComputationNode node) {
    return map.get(node.getComputationName());
  }
}
