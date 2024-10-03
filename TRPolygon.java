package ctrmap.formats.pokemon.gen8.model;

import ctrmap.renderer.scene.model.Mesh;
import ctrmap.renderer.scene.model.Vertex;
import ctrmap.renderer.scene.model.VertexMorph;
import ctrmap.renderer.scene.model.draw.vtxlist.AbstractVertexList;
import ctrmap.renderer.scene.model.draw.vtxlist.MorphableVertexList;
import ctrmap.renderer.util.PrimitiveConverter;
import ctrmap.renderer.util.VBOProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import xstandard.io.util.BitConverter;
import xstandard.math.HalfFloat;
import xstandard.math.vec.RGBA;
import xstandard.math.vec.Vec2f;
import xstandard.math.vec.Vec3f;
import xstandard.math.vec.Vec4f;
import xstandard.util.collections.IntList;

public class TRPolygon {
  public String polygonName;
  
  public TRBoundingBox bbox;
  
  public TRIBOType iboType;
  
  public List<TRVtxDataDescriptor> vboDescriptors;
  
  public List<TRMaterialMapping> materials;
  
  public int res0;
  
  public int res1;
  
  public int res2;
  
  public int res3;
  
  public Vec4f center;
  
  public List<TRBoneInfluence> bones;
  
  public List<TRVertexMorphRef> morphRefs;
  
  public String visGroupName;
  
  public int unk8;
  
  public List<TRVertexMorphGroup> morphGroups;
  
  private static final float _1_255 = 0.003921569F;
  
  private static final float _1_65535 = 1.5259022E-5F;
  
  public TRPolygon() {}
  
  public TRPolygon(TRSkeleton skeleton, TRVertexBufferSet.Buffer outBuffer, Mesh... meshes) {
    this.polygonName = (meshes[0]).name;
    this.visGroupName = (meshes[0]).visGroupName;
    if (this.visGroupName == null)
      this.visGroupName = ""; 
    this.morphRefs = new ArrayList<>();
    this.morphGroups = new ArrayList<>();
    this.bbox = new TRBoundingBox();
    this.bbox.max = new Vec3f(-3.4028235E38F);
    this.bbox.min = new Vec3f(Float.MAX_VALUE);
    this.bones = new ArrayList<>();
    this.materials = new ArrayList<>();
    TRVtxDataDescriptor descriptor = new TRVtxDataDescriptor();
    Map<Integer, Integer> jointRemap = new HashMap<>();
    for (int i = 0; i < skeleton.bones.size(); i++)
      jointRemap.put(Integer.valueOf(i), Integer.valueOf(((TRBone)skeleton.bones.get(i)).ibpIdx)); 
    int indexTotal = 0;
    int vertexTotal = 0;
    Set<Integer> boneIDs = new HashSet<>();
    Mesh[] readyMeshes = new Mesh[meshes.length];
    for (int j = 0; j < meshes.length; j++) {
      Mesh mesh = meshes[j];
      mesh = PrimitiveConverter.getTriMesh(mesh);
      if (!mesh.useIBO) {
        mesh = new Mesh(mesh);
        VBOProcessor.makeIndexed(mesh, false);
        System.out.println("indexing...");
      } 
      readyMeshes[j] = mesh;
      indexTotal += mesh.indices.size();
      vertexTotal += mesh.getRealVertexCount();
      for (Vertex vtx : mesh.getVertexArrays()[0]) {
        for (int bidxi = 0; bidxi < vtx.boneIndices.size(); bidxi++)
          boneIDs.add(Integer.valueOf(vtx.boneIndices.get(bidxi))); 
      } 
    } 
    for (Iterator<Integer> iterator = boneIDs.iterator(); iterator.hasNext(); ) {
      int boneID = ((Integer)iterator.next()).intValue();
      TRBoneInfluence inf = new TRBoneInfluence();
      inf.index = ((Integer)jointRemap.get(Integer.valueOf(boneID))).intValue();
      inf.scale = 50.0F;
      this.bones.add(inf);
    } 
    this.iboType = (indexTotal > 65536) ? TRIBOType.U32 : TRIBOType.U16;
    byte[] ibo = new byte[this.iboType.sizeof * indexTotal];
    int outIboIndex = 0;
    Mesh refMesh = meshes[0];
    int attrOffset = 0;
    descriptor.attributes = new ArrayList<>();
    descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.POSITION, TRVtxAttrFormat.VEC3_F32, 0, attrOffset));
    attrOffset += TRVtxAttrFormat.VEC3_F32.sizeof;
    if (refMesh.hasNormal) {
      descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.NORMAL, TRVtxAttrFormat.VEC4_F16, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_F16.sizeof;
    } 
    if (refMesh.hasTangent) {
      descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.TANGENT, TRVtxAttrFormat.VEC4_F16, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_F16.sizeof;
    } 
    for (int k = 0; k < refMesh.hasUV.length; k++) {
      if (refMesh.hasUV(k)) {
        descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.TEX_COORD, TRVtxAttrFormat.VEC2_F32, k, attrOffset));
        attrOffset += TRVtxAttrFormat.VEC2_F32.sizeof;
      } 
    } 
    if (refMesh.hasColor) {
      descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.COLOR, TRVtxAttrFormat.VEC4_UNORM8, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_UNORM8.sizeof;
    } 
    if (refMesh.hasBoneIndices) {
      descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.BONE_INDICES, TRVtxAttrFormat.VEC4_U8, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_U8.sizeof;
      descriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.BONE_WEIGHTS, TRVtxAttrFormat.VEC4_UNORM16, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_UNORM16.sizeof;
    } 
    int vtxStride = attrOffset;
    descriptor.strides = new ArrayList<>();
    descriptor.strides.add(new TRVtxStride(vtxStride));
    byte[] vbo = new byte[vtxStride * vertexTotal];
    int outVboOffset = 0;
    int outVboVtxCount = 0;
    int maxMorphCount = 0;
    for (Mesh mesh : readyMeshes) {
      this.bbox.min.min((Vector3fc)mesh.calcMinVector());
      this.bbox.max.max((Vector3fc)mesh.calcMaxVector());
      TRMaterialMapping mat = new TRMaterialMapping();
      mat.materialName = mesh.materialName;
      mat.firstIndex = outIboIndex;
      mat.indexCount = mesh.indices.size();
      this.materials.add(mat);
      int outPos;
      for (int m = 0; m < mesh.indices.size(); m++, outPos += this.iboType.sizeof)
        BitConverter.fromIntLE(mesh.indices.get(m) + outVboVtxCount, ibo, outPos, this.iboType.sizeof); 
      outIboIndex += mesh.indices.size();
      AbstractVertexList vertices = mesh.getVertexArrays()[0];
      maxMorphCount = Math.max(maxMorphCount, (mesh.getVertexArrays()).length);
      outVboOffset = encodeVBO(vertices, descriptor, vbo, outVboOffset, jointRemap);
      outVboVtxCount += vertices.size();
    } 
    outBuffer.ibos.add(new TRVertexBufferSet.TRRawData(ibo));
    outBuffer.vbos.add(new TRVertexBufferSet.TRRawData(vbo));
    this.vboDescriptors = new ArrayList<>();
    this.vboDescriptors.add(descriptor);
    if (maxMorphCount > 1) {
      Map<String, List<VertexMorphInput>> groupedMorphs = new LinkedHashMap<>();
      for (int morphIndex = 1; morphIndex < maxMorphCount; morphIndex++) {
        String morphName = ((VertexMorph)((MorphableVertexList)refMesh.vertices).morphs().get(morphIndex)).name;
        VertexMorphInput vertexMorphInput = new VertexMorphInput();
        vertexMorphInput.name = morphName;
        for (Mesh m : readyMeshes) {
          AbstractVertexList[] varr = m.getVertexArrays();
          AbstractVertexList vertices = varr[(morphIndex < varr.length) ? morphIndex : 0];
          vertexMorphInput.vertArrays.add(vertices);
        } 
        boolean grouped = morphName.contains("/");
        if (grouped) {
          String groupName = morphName.substring(0, morphName.indexOf('/'));
          List<VertexMorphInput> group = groupedMorphs.get(groupName);
          if (group == null) {
            group = new ArrayList<>();
            groupedMorphs.put(groupName, group);
          } 
          vertexMorphInput.name = vertexMorphInput.name.substring(groupName.length() + 1);
          group.add(vertexMorphInput);
        } else {
          TRVertexMorphRef mr = new TRVertexMorphRef();
          mr.name = morphName;
          mr.index = morphIndex;
          this.morphRefs.add(mr);
          TRVtxDataDescriptor morphDescriptor = createMorphDescriptor(refMesh);
          int morphStride = ((TRVtxStride)morphDescriptor.strides.get(0)).bytes;
          byte[] morphVbo = new byte[morphStride * vertexTotal];
          outVboOffset = 0;
          for (AbstractVertexList vertices : vertexMorphInput.vertArrays) {
            outVboOffset = encodeVBO(vertices, morphDescriptor, morphVbo, outVboOffset, jointRemap);
            outVboVtxCount += vertices.size();
          } 
          if (outVboOffset != morphVbo.length)
            throw new RuntimeException("VBO truncated"); 
          outBuffer.vbos.add(new TRVertexBufferSet.TRRawData(morphVbo));
          this.vboDescriptors.add(morphDescriptor);
        } 
      } 
      Set<Integer> groupModIndices = new HashSet<>();
      for (Map.Entry<String, List<VertexMorphInput>> e : groupedMorphs.entrySet()) {
        TRVertexMorphGroup g = new TRVertexMorphGroup();
        g.morphRefs = new ArrayList<>();
        g.vboDescriptors = new ArrayList<>();
        g.name = e.getKey();
        TRVtxDataDescriptor mapperDesc = new TRVtxDataDescriptor();
        mapperDesc.strides = new ArrayList<>();
        mapperDesc.strides.add(new TRVtxStride(4));
        mapperDesc.attributes = new ArrayList<>();
        mapperDesc.attributes.add(new TRVtxAttr(TRVtxAttrName.POINT_SIZE, TRVtxAttrFormat.U32, 0, 0));
        g.vboDescriptors.add(mapperDesc);
        Set<Integer> modifiedIndices = new HashSet<>();
        AbstractVertexList[] refArrays = new AbstractVertexList[readyMeshes.length];
        for (int m = 0; m < refArrays.length; m++)
          refArrays[m] = readyMeshes[m].getVertexArrays()[0]; 
        for (VertexMorphInput vm : e.getValue()) {
          int vboIndex = 0;
          for (int arrIdx = 0; arrIdx < vm.vertArrays.size(); arrIdx++) {
            AbstractVertexList arr = vm.vertArrays.get(arrIdx);
            AbstractVertexList refArr = refArrays[arrIdx];
            for (int vIdx = 0; vIdx < arr.size(); vIdx++) {
              Vertex vert = (Vertex)arr.get(vIdx);
              Vertex refVert = (Vertex)refArr.get(vIdx);
              boolean changed = (!vert.position.equalsImprecise(refVert.position, 1.0E-4F) || (vert.normal != null && refMesh.hasNormal && !vert.normal.equalsImprecise(refVert.normal, 1.0E-4F)) || (vert.tangent != null && refMesh.hasTangent && !vert.tangent.equalsImprecise(refVert.tangent, 1.0E-4F)));
              if (changed)
                modifiedIndices.add(Integer.valueOf(vboIndex)); 
              vboIndex++;
            } 
          } 
        } 
        for (Iterator<Integer> iterator1 = modifiedIndices.iterator(); iterator1.hasNext(); ) {
          int mi = ((Integer)iterator1.next()).intValue();
          if (groupModIndices.contains(Integer.valueOf(mi))) {
            System.err.println("Warning: multiple morph groups target the same vertex. Behavior is undefined.");
            continue;
          } 
          groupModIndices.add(Integer.valueOf(mi));
        } 
        List<Integer> modIndicesOrdered = new ArrayList<>(modifiedIndices);
        byte[] modIndicesBuf = new byte[modIndicesOrdered.size() << 2];
        for (int n = 0; n < modIndicesOrdered.size(); n++)
          BitConverter.fromInt32LE(((Integer)modIndicesOrdered.get(n)).intValue(), modIndicesBuf, n << 2); 
        TRVertexBufferSet.TRRawData mainVBO = new TRVertexBufferSet.TRRawData(modIndicesBuf);
        TRVertexBufferSet.TRMorphVBOGroup vboGroup = new TRVertexBufferSet.TRMorphVBOGroup();
        vboGroup.vbos = new ArrayList<>();
        vboGroup.vbos.add(mainVBO);
        int vertTotal = 0;
        for (AbstractVertexList vl : refArrays)
          vertTotal += vl.size(); 
        Vertex[] refVerts = new Vertex[vertTotal];
        int vertIdx = 0;
        for (AbstractVertexList vl : refArrays) {
          for (Vertex v : vl)
            refVerts[vertIdx++] = v; 
        } 
        for (VertexMorphInput in : e.getValue()) {
          TRVtxDataDescriptor desc = createMorphDescriptor(refMesh);
          TRVertexMorph morph = new TRVertexMorph();
          morph.vboIndex = g.vboDescriptors.size();
          g.vboDescriptors.add(desc);
          morph.type = 1;
          morph.name = in.name;
          g.morphRefs.add(morph);
          int morphStride = ((TRVtxStride)desc.strides.get(0)).bytes;
          byte[] morphVbo = new byte[morphStride * modIndicesOrdered.size()];
          Vertex[] srcVerts = new Vertex[vertTotal];
          int i1 = 0;
          for (AbstractVertexList vl : in.vertArrays) {
            for (Vertex v : vl)
              srcVerts[i1++] = v; 
          } 
          outVboOffset = 0;
          Vertex comb = new Vertex();
          comb.normal = new Vec3f();
          comb.tangent = new Vec3f();
          for (int i2 = 0; i2 < modIndicesOrdered.size(); i2++) {
            Vertex base = refVerts[((Integer)modIndicesOrdered.get(i2)).intValue()];
            Vertex mv = srcVerts[((Integer)modIndicesOrdered.get(i2)).intValue()];
            mv.position.sub((Vector3fc)base.position, (Vector3f)comb.position);
            if (mv.normal != null)
              mv.normal.sub((Vector3fc)base.normal, (Vector3f)comb.normal); 
            if (mv.tangent != null)
              mv.tangent.sub((Vector3fc)base.tangent, (Vector3f)comb.tangent); 
            outVboOffset = encodeVertex(comb, desc, morphVbo, outVboOffset, jointRemap);
          } 
          vboGroup.vbos.add(new TRVertexBufferSet.TRRawData(morphVbo));
        } 
        outBuffer.morphs.add(vboGroup);
        this.morphGroups.add(g);
      } 
    } 
    this.center = this.bbox.min.clone().add(this.bbox.max).mul(0.5F).toVec4();
  }
  
  private TRVtxDataDescriptor createMorphDescriptor(Mesh refMesh) {
    TRVtxDataDescriptor morphDescriptor = new TRVtxDataDescriptor();
    int attrOffset = 0;
    morphDescriptor.attributes = new ArrayList<>();
    morphDescriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.POSITION, TRVtxAttrFormat.VEC3_F32, 0, attrOffset));
    attrOffset += TRVtxAttrFormat.VEC3_F32.sizeof;
    if (refMesh.hasNormal) {
      morphDescriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.NORMAL, TRVtxAttrFormat.VEC4_F16, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_F16.sizeof;
    } 
    if (refMesh.hasTangent) {
      morphDescriptor.attributes.add(new TRVtxAttr(TRVtxAttrName.TANGENT, TRVtxAttrFormat.VEC4_F16, 0, attrOffset));
      attrOffset += TRVtxAttrFormat.VEC4_F16.sizeof;
    } 
    int morphStride = attrOffset;
    morphDescriptor.strides = new ArrayList<>();
    morphDescriptor.strides.add(new TRVtxStride(morphStride));
    return morphDescriptor;
  }
  
  private Vec4f srcValue = new Vec4f();
  
  private int encodeVBO(AbstractVertexList vertices, TRVtxDataDescriptor descriptor, byte[] vbo, int outVboOffset, Map<Integer, Integer> jointRemap) {
    for (int vIdx = 0; vIdx < vertices.size(); vIdx++) {
      Vertex vtx = (Vertex)vertices.get(vIdx);
      outVboOffset = encodeVertex(vtx, descriptor, vbo, outVboOffset, jointRemap);
    } 
    return outVboOffset;
  }
  
  private int encodeVertex(Vertex vtx, TRVtxDataDescriptor descriptor, byte[] vbo, int outVboOffset, Map<Integer, Integer> jointRemap) {
    for (TRVtxAttr a : descriptor.attributes) {
      Vec2f uv;
      int i;
      switch (a.name) {
        case VEC2_F32:
          this.srcValue.set((Vector3fc)vtx.position, 1.0F);
          break;
        case VEC3_F32:
          this.srcValue.set((Vector3fc)vtx.normal, 0.0F);
          break;
        case VEC4_F32:
          vtx.color.toVector4(this.srcValue);
          break;
        case VEC4_F16:
          this.srcValue.set((Vector3fc)vtx.tangent, 0.0F);
          break;
        case VEC4_U8:
          uv = vtx.uv[a.setNo];
          this.srcValue.set(uv.x, uv.y, 0.0F, 0.0F);
          break;
        case VEC4_UNORM8:
          for (i = 0; i < 4; i++) {
            if (i < vtx.boneIndices.size()) {
              this.srcValue.setComponent(i, ((Integer)jointRemap.get(Integer.valueOf(vtx.boneIndices.get(i)))).intValue());
            } else {
              this.srcValue.setComponent(i, 0.0F);
            } 
          } 
          break;
        case VEC4_UNORM16:
          for (i = 0; i < 4; i++) {
            if (i < vtx.weights.size()) {
              this.srcValue.setComponent(i, vtx.weights.get(i));
            } else {
              this.srcValue.setComponent(i, 0.0F);
            } 
          } 
          break;
      } 
      switch (a.format) {
        case VEC2_F32:
          BitConverter.fromFloatLE(this.srcValue.x, vbo, outVboOffset + 0);
          BitConverter.fromFloatLE(this.srcValue.y, vbo, outVboOffset + 4);
          break;
        case VEC3_F32:
          BitConverter.fromFloatLE(this.srcValue.x, vbo, outVboOffset + 0);
          BitConverter.fromFloatLE(this.srcValue.y, vbo, outVboOffset + 4);
          BitConverter.fromFloatLE(this.srcValue.z, vbo, outVboOffset + 8);
          break;
        case VEC4_F32:
          BitConverter.fromFloatLE(this.srcValue.x, vbo, outVboOffset + 0);
          BitConverter.fromFloatLE(this.srcValue.y, vbo, outVboOffset + 4);
          BitConverter.fromFloatLE(this.srcValue.z, vbo, outVboOffset + 8);
          BitConverter.fromFloatLE(this.srcValue.w, vbo, outVboOffset + 12);
          break;
        case VEC4_F16:
          BitConverter.fromInt16LE(HalfFloat.fromFloat(this.srcValue.x), vbo, outVboOffset + 0);
          BitConverter.fromInt16LE(HalfFloat.fromFloat(this.srcValue.y), vbo, outVboOffset + 2);
          BitConverter.fromInt16LE(HalfFloat.fromFloat(this.srcValue.z), vbo, outVboOffset + 4);
          BitConverter.fromInt16LE(HalfFloat.fromFloat(this.srcValue.w), vbo, outVboOffset + 6);
          break;
        case VEC4_U8:
          vbo[outVboOffset + 0] = (byte)(int)this.srcValue.x;
          vbo[outVboOffset + 1] = (byte)(int)this.srcValue.y;
          vbo[outVboOffset + 2] = (byte)(int)this.srcValue.z;
          vbo[outVboOffset + 3] = (byte)(int)this.srcValue.w;
          break;
        case VEC4_UNORM8:
          vbo[outVboOffset + 0] = (byte)(int)(this.srcValue.x * 255.0F);
          vbo[outVboOffset + 1] = (byte)(int)(this.srcValue.y * 255.0F);
          vbo[outVboOffset + 2] = (byte)(int)(this.srcValue.z * 255.0F);
          vbo[outVboOffset + 3] = (byte)(int)(this.srcValue.w * 255.0F);
          break;
        case VEC4_UNORM16:
          BitConverter.fromInt16LE((int)(this.srcValue.x * 65535.0F), vbo, outVboOffset + 0);
          BitConverter.fromInt16LE((int)(this.srcValue.y * 65535.0F), vbo, outVboOffset + 2);
          BitConverter.fromInt16LE((int)(this.srcValue.z * 65535.0F), vbo, outVboOffset + 4);
          BitConverter.fromInt16LE((int)(this.srcValue.w * 65535.0F), vbo, outVboOffset + 6);
          break;
      } 
      outVboOffset += a.format.sizeof;
    } 
    return outVboOffset;
  }
  
  public List<Mesh> toMeshes(TRVertexBufferSet.Buffer buffers) {
    List<Mesh> meshes = new ArrayList<>();
    Map<Integer, Integer> boneIdxRemap = new HashMap<>();
    for (int i = 0; i < this.bones.size(); i++)
      boneIdxRemap.put(Integer.valueOf(i), Integer.valueOf(((TRBoneInfluence)this.bones.get(i)).index)); 
    List<TRVertexBufferSet.TRRawData> allVbos = new ArrayList<>();
    List<TRVtxDataDescriptor> allVboDesc = new ArrayList<>();
    Map<TRVtxDataDescriptor, String> morphNames = new HashMap<>();
    allVbos.addAll(buffers.vbos);
    allVboDesc.addAll(this.vboDescriptors);
    if (allVboDesc.size() != allVbos.size())
      throw new RuntimeException(); 
    morphNames.put(this.vboDescriptors.get(0), this.polygonName);
    for (TRVertexMorphRef r : this.morphRefs)
      morphNames.put(this.vboDescriptors.get(r.index), r.name); 
    Map<TRVtxDataDescriptor, int[]> morphIndexers = (Map)new HashMap<>();
    if (this.morphGroups != null) {
      int gidx = 0;
      for (TRVertexMorphGroup g : this.morphGroups) {
        byte[] indexer = ((TRVertexBufferSet.TRRawData)((TRVertexBufferSet.TRMorphVBOGroup)buffers.morphs.get(gidx)).vbos.get(0)).rawData;
        int[] patch = new int[indexer.length / 4];
        for (int j = 0; j < patch.length; j++)
          patch[j] = BitConverter.toInt32LE(indexer, j << 2); 
        for (TRVertexMorph m : g.morphRefs) {
          allVboDesc.add(g.vboDescriptors.get(m.vboIndex));
          allVbos.add(((TRVertexBufferSet.TRMorphVBOGroup)buffers.morphs.get(gidx)).vbos.get(m.vboIndex));
          morphNames.put(g.vboDescriptors.get(m.vboIndex), g.name + "/" + m.name);
          morphIndexers.put(g.vboDescriptors.get(m.vboIndex), patch);
        } 
        gidx++;
      } 
    } 
    for (TRMaterialMapping face : this.materials) {
      Mesh mesh = new Mesh();
      mesh.name = this.polygonName;
      if (this.visGroupName == null || this.visGroupName.length() == 0) {
        mesh.visGroupName = null;
      } else {
        mesh.visGroupName = this.visGroupName;
      } 
      mesh.useIBO = true;
      mesh.materialName = face.materialName;
      if (allVboDesc.size() > 1)
        mesh.vertices = (AbstractVertexList)new MorphableVertexList(); 
      for (TRVtxAttr a : ((TRVtxDataDescriptor)this.vboDescriptors.get(0)).attributes) {
        switch (a.name) {
          case VEC3_F32:
            mesh.hasNormal = true;
            break;
          case VEC4_F16:
            mesh.hasTangent = true;
            break;
          case VEC4_F32:
            mesh.hasColor = true;
            break;
          case VEC4_U8:
            mesh.hasUV[a.setNo] = true;
            break;
          case VEC4_UNORM8:
            mesh.hasBoneIndices = true;
            break;
          case VEC4_UNORM16:
            mesh.hasBoneWeights = true;
            break;
        } 
        switch (a.format) {
          case VEC2_F32:
          case VEC3_F32:
          case VEC4_F32:
          case VEC4_F16:
          case VEC4_U8:
          case VEC4_UNORM8:
          case VEC4_UNORM16:
            continue;
        } 
        System.err.println("UNKNOWN VERTEX ATTRIBUTE FORMAT: " + a.format + " (name " + a.name + " offset " + a.offset + " vertex stride " + ((TRVtxDataDescriptor)this.vboDescriptors.get(0)).strides.get(0) + ") @ polygon " + this.polygonName);
      } 
      byte[] iboRaw = ((TRVertexBufferSet.TRRawData)buffers.ibos.get(0)).rawData;
      if (buffers.ibos.size() > 1)
        System.err.println("WARN: More than 1 index buffer specified - unknown behavior. (" + buffers.ibos.size() + ")"); 
      int iboStride = this.iboType.sizeof;
      int[] iboDecoded = new int[face.indexCount];
      int inOfs;
      for (int j = 0; j < face.indexCount; j++, inOfs += iboStride)
        iboDecoded[j] = BitConverter.toIntLE(iboRaw, inOfs, iboStride); 
      Map<Integer, Integer> iboRemap = new HashMap<>();
      IntList requiredIndices = new IntList();
      for (int k = 0, outVtxIdx = 0; k < iboDecoded.length; k++) {
        int rawIndex = iboDecoded[k];
        int index = ((Integer)iboRemap.getOrDefault(Integer.valueOf(rawIndex), Integer.valueOf(-1))).intValue();
        if (index == -1) {
          index = outVtxIdx;
          iboRemap.put(Integer.valueOf(rawIndex), Integer.valueOf(index));
          requiredIndices.add(rawIndex);
          outVtxIdx++;
        } 
        mesh.indices.add(index);
      } 
      for (int vertexMorphIndex = 0; vertexMorphIndex < allVboDesc.size(); vertexMorphIndex++) {
        AbstractVertexList dest;
        if (allVboDesc.size() > 1) {
          VertexMorph morph = new VertexMorph();
          morph.name = morphNames.getOrDefault(allVboDesc.get(vertexMorphIndex), this.polygonName);
          ((MorphableVertexList)mesh.vertices).addMorph(morph);
          dest = morph.vertices;
        } else {
          dest = mesh.vertices;
        } 
        int mainVboIndex = vertexMorphIndex;
        TRVtxDataDescriptor morphDesc = allVboDesc.get(vertexMorphIndex);
        int[] morphPatch = morphIndexers.get(morphDesc);
        if (morphPatch != null)
          mainVboIndex = 0; 
        byte[] vboRaw = ((TRVertexBufferSet.TRRawData)allVbos.get(mainVboIndex)).rawData;
        TRVtxDataDescriptor vboDesc = allVboDesc.get(mainVboIndex);
        int vtxStride = ((TRVtxStride)vboDesc.strides.get(0)).bytes;
        if (vboDesc.strides.size() > 1)
          System.err.println("WARN: More than 1 vertex stride specified - unknown behavior. (" + vboDesc.strides.size() + ")"); 
        Set<TRVtxAttrName> reasonableAttrs = new HashSet<>();
        for (TRVtxAttr a : morphDesc.attributes)
          reasonableAttrs.add(a.name); 
        List<TRVtxAttr> usedAttrs = new ArrayList<>();
        for (TRVtxAttr a : vboDesc.attributes) {
          if (reasonableAttrs.contains(a.name))
            usedAttrs.add(a); 
        } 
        Vec4f decodeVector = new Vec4f();
        for (int m = 0; m < requiredIndices.size(); m++) {
          int idxInVbo = requiredIndices.get(m);
          byte[] indata = vboRaw;
          int vtxStart = vtxStride * idxInVbo;
          Vertex vert = new Vertex();
          for (TRVtxAttr a : usedAttrs) {
            decodeAttribute(a.format, indata, vtxStart + a.offset, decodeVector);
            setAttribute(a.name, a.setNo, decodeVector, vert);
          } 
          vert.boneIndices.trimToSize(vert.weights.size());
          dest.add(vert);
        } 
        if (morphPatch != null) {
          byte[] indata = ((TRVertexBufferSet.TRRawData)allVbos.get(vertexMorphIndex)).rawData;
          int morphStride = ((TRVtxStride)morphDesc.strides.get(0)).bytes;
          for (int n = 0; n < morphPatch.length; n++) {
            int actualIndex = ((Integer)iboRemap.getOrDefault(Integer.valueOf(morphPatch[n]), Integer.valueOf(-1))).intValue();
            if (actualIndex != -1) {
              Vertex vert = (Vertex)dest.get(actualIndex);
              int vtxStart = morphStride * n;
              for (TRVtxAttr a : morphDesc.attributes) {
                decodeAttribute(a.format, indata, vtxStart + a.offset, decodeVector);
                addAttribute(a.name, a.setNo, decodeVector, vert);
              } 
            } 
          } 
        } 
      } 
      meshes.add(mesh);
    } 
    return meshes;
  }
  
  private static void decodeAttribute(TRVtxAttrFormat format, byte[] indata, int offset, Vec4f dest) {
    switch (format) {
      case VEC2_F32:
        dest.x = BitConverter.toFloatLE(indata, offset + 0);
        dest.y = BitConverter.toFloatLE(indata, offset + 4);
        break;
      case VEC3_F32:
        dest.x = BitConverter.toFloatLE(indata, offset + 0);
        dest.y = BitConverter.toFloatLE(indata, offset + 4);
        dest.z = BitConverter.toFloatLE(indata, offset + 8);
        break;
      case VEC4_F32:
        dest.x = BitConverter.toFloatLE(indata, offset + 0);
        dest.y = BitConverter.toFloatLE(indata, offset + 4);
        dest.z = BitConverter.toFloatLE(indata, offset + 8);
        dest.w = BitConverter.toFloatLE(indata, offset + 12);
        break;
      case VEC4_U8:
        dest.x = (indata[offset + 0] & 0xFF);
        dest.y = (indata[offset + 1] & 0xFF);
        dest.z = (indata[offset + 2] & 0xFF);
        dest.w = (indata[offset + 3] & 0xFF);
        break;
      case VEC4_UNORM8:
        dest.x = (indata[offset] & 0xFF);
        dest.y = (indata[offset + 1] & 0xFF);
        dest.z = (indata[offset + 2] & 0xFF);
        dest.w = (indata[offset + 3] & 0xFF);
        dest.mul(0.003921569F);
        break;
      case VEC4_F16:
        dest.x = HalfFloat.toFloat(BitConverter.toInt16LE(indata, offset + 0));
        dest.y = HalfFloat.toFloat(BitConverter.toInt16LE(indata, offset + 2));
        dest.z = HalfFloat.toFloat(BitConverter.toInt16LE(indata, offset + 4));
        dest.w = HalfFloat.toFloat(BitConverter.toInt16LE(indata, offset + 6));
        break;
      case VEC4_UNORM16:
        dest.x = (BitConverter.toInt16LE(indata, offset + 0) & 0xFFFF);
        dest.y = (BitConverter.toInt16LE(indata, offset + 2) & 0xFFFF);
        dest.z = (BitConverter.toInt16LE(indata, offset + 4) & 0xFFFF);
        dest.w = (BitConverter.toInt16LE(indata, offset + 6) & 0xFFFF);
        dest.mul(1.5259022E-5F);
        break;
    } 
  }
  
  private static void setAttribute(TRVtxAttrName name, int setNo, Vec4f src, Vertex dest) {
    int wcount;
    int wIdx;
    switch (name) {
      case VEC2_F32:
        dest.position.set(src.x, src.y, src.z);
        break;
      case VEC4_F16:
        if (dest.tangent == null)
          dest.tangent = new Vec3f(src.x, src.y, src.z); 
        break;
      case VEC3_F32:
        dest.normal = new Vec3f(src.x, src.y, src.z);
        break;
      case VEC4_U8:
        dest.uv[setNo] = new Vec2f(src.x, src.y);
        break;
      case VEC4_F32:
        dest.color = new RGBA(src);
        break;
      case VEC4_UNORM16:
        wcount = 0;
        for (wIdx = 3; wIdx >= 0; wIdx--) {
          float w = src.get(wIdx);
          if (w != 0.0F) {
            wcount = wIdx + 1;
            break;
          } 
        } 
        for (wIdx = 0; wIdx < wcount; wIdx++) {
          float w = src.get(wIdx);
          dest.weights.add(w);
        } 
        break;
      case VEC4_UNORM8:
        for (wIdx = 0; wIdx < 4; ) {
          int boneIdx = (int)src.get(wIdx);
          if (boneIdx != 255) {
            dest.boneIndices.add(boneIdx);
            wIdx++;
          } 
        } 
        break;
    } 
  }
  
  private static void addAttribute(TRVtxAttrName name, int setNo, Vec4f src, Vertex dest) {
    switch (name) {
      case VEC2_F32:
        dest.position.add(src.x, src.y, src.z);
        break;
      case VEC4_F16:
        dest.tangent.add(src.x, src.y, src.z);
        break;
      case VEC3_F32:
        dest.normal.add(src.x, src.y, src.z);
        break;
      case VEC4_U8:
        dest.uv[setNo].add(src.x, src.y);
        break;
    } 
  }
  
  private static class VertexMorphInput {
    public String name;
    
    private VertexMorphInput() {}
    
    public List<AbstractVertexList> vertArrays = new ArrayList<>();
  }
}
