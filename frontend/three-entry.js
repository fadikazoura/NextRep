// Only the parts of three.js NextRep's 3D muscle map uses; esbuild tree-shakes the rest away.
export {
    WebGLRenderer, Scene, PerspectiveCamera, Group, Mesh, SphereGeometry, CapsuleGeometry, CylinderGeometry,
    PlaneGeometry, CircleGeometry, MeshStandardMaterial, ShadowMaterial, MeshBasicMaterial, HemisphereLight, DirectionalLight,
    AmbientLight, Color, Raycaster, Vector2, Vector3, PCFShadowMap, SRGBColorSpace, ACESFilmicToneMapping,
} from 'three';
