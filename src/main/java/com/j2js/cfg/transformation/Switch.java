package com.j2js.cfg.transformation;

import java.util.ArrayList;
import java.util.List;

import com.j2js.cfg.Edge;
import com.j2js.cfg.Node;
import com.j2js.cfg.SwitchEdge;
import com.j2js.dom.Block;
import com.j2js.dom.NumberLiteral;
import com.j2js.dom.SwitchCase;
import com.j2js.dom.SwitchStatement;

/**
 */
public class Switch extends Transformation {

    private List<Node> caseGroups = new ArrayList<Node>();
    private List<List<NumberLiteral>> caseGroupExpressions = new ArrayList<List<NumberLiteral>>();
    
    public boolean applies_() {
        return header.isSwitchHeader;
    }
    
    private void removeFallThroughEdgesl() {
        Edge prevPotentialFallThroughEdge = null;
        
        for (Edge e : header.getOutEdges()) {
            if (!(e instanceof SwitchEdge)) continue;
            
            SwitchEdge edge = (SwitchEdge) e;
            Node caseGroup = edge.target;

            if (prevPotentialFallThroughEdge != null && prevPotentialFallThroughEdge.target == caseGroup) {
                // This is a fall through edge.
                graph.removeEdge(prevPotentialFallThroughEdge);
            }

            prevPotentialFallThroughEdge = caseGroup.getLocalOutEdgeOrNull();
        }
    }
    
    void apply_() {
        removeFallThroughEdgesl();
        
        for (Edge e : new ArrayList<Edge>(header.getOutEdges())) {
            if (!(e instanceof SwitchEdge)) continue;
            
            SwitchEdge edge = (SwitchEdge) e;
            Node caseGroup = edge.target;
            caseGroups.add(caseGroup);
            caseGroupExpressions.add(edge.expressions);
            graph.rerootOutEdges(caseGroup, newNode, true);
            graph.removeOutEdges(caseGroup);
            graph.removeInEdges(caseGroup);
            graph.removeNode(caseGroup);
        }
    }
    
    void rollOut_(Block block) {
        SwitchStatement switchStmt = new SwitchStatement();
        switchStmt.setExpression(header.switchExpression);
        
        for (int i=0; i<caseGroups.size(); i++) {
            Node scNode = caseGroups.get(i);
            SwitchCase switchCase = new SwitchCase(scNode.getInitialPc());
            switchCase.setExpressions(caseGroupExpressions.get(i));
            switchStmt.appendChild(switchCase);
            
            graph.rollOut(scNode, switchCase);
        }

        block.appendChild(switchStmt);
    }
    
    public String toString() {
        String s = super.toString() + "(" + header;
        for (int i=0; i<caseGroups.size(); i++) {
            s += ", " + caseGroups.get(i);
        }
        return s + ")";
    }
}
