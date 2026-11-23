import {
    WebGLRenderer, Scene, PerspectiveCamera, Group, Mesh, SphereGeometry, CircleGeometry, MeshStandardMaterial,
    ShadowMaterial, HemisphereLight, DirectionalLight, Color, Raycaster, Vector2, Vector3, PCFShadowMap, SRGBColorSpace,
    ACESFilmicToneMapping,
} from './vendor/three.min.js';

/*
 * NextRep 3D muscle map: an anatomical figure generated in code (no model file to download, a few KB of JS).
 * The body is made of "lathed" shapes: a sphere whose radius follows a profile curve along its height, so the torso
 * gets a real V-taper and the limbs get muscle bellies. Every muscle group is made of separate, clickable meshes
 * (pecs, delts, abs, lats, quads, ...) that sit slightly into the skin, so the gaps between them read as definition.
 * Units: the figure is ~4.15 tall and stands on y = 0, facing +z.
 */

export const MUSCLE_GROUPS = ['CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'QUADS', 'HAMSTRINGS', 'GLUTES', 'CALVES', 'CORE'];

const UP = new Vector3(0, 1, 0);
const FRONT = new Vector3(0, 0, 1);

function cssColor(name, fallback) {
    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return new Color(value || fallback);
}

/** Smooth curve through [t, value] points (cubic Hermite with Catmull-Rom tangents). */
function curve(points) {
    const n = points.length;
    const slope = (i) => {
        const a = points[Math.max(0, i - 1)];
        const b = points[Math.min(n - 1, i + 1)];
        return (b[1] - a[1]) / (b[0] - a[0]);
    };
    return (t) => {
        if (t <= points[0][0]) return points[0][1];
        for (let i = 1; i < n; i++) {
            const [t1, v1] = points[i];
            if (t > t1) continue;
            const [t0, v0] = points[i - 1];
            const dt = t1 - t0;
            const u = (t - t0) / dt;
            const u2 = u * u;
            const u3 = u2 * u;
            return (2 * u3 - 3 * u2 + 1) * v0 + (u3 - 2 * u2 + u) * dt * slope(i - 1)
                + (-2 * u3 + 3 * u2) * v1 + (u3 - u2) * dt * slope(i);
        }
        return points[n - 1][1];
    };
}
const constant = (v) => () => v;

/**
 * A closed, smooth shape along +y from 0 to `length`. rx/rz give the half-width and half-depth at t (0..1),
 * ox/oz shift the center (e.g. a calf that bulges backwards). `cap` controls how round the two ends are.
 */
function lathe({ length, rx, rz = rx, ox = constant(0), oz = constant(0), cap = 0.22, segments = 40, rings = 40 }) {
    const geometry = new SphereGeometry(1, segments, rings);
    const pos = geometry.attributes.position;
    for (let i = 0; i < pos.count; i++) {
        const x = pos.getX(i);
        const y = pos.getY(i);
        const z = pos.getZ(i);
        const t = (y + 1) / 2; // t = 0 at local y = 0 (start of the shape), 1 at y = length
        const len = Math.hypot(x, z) || 1;
        const round = Math.pow(Math.max(0, 1 - y * y), cap);
        pos.setXYZ(i, ox(t) + (x / len) * rx(t) * round, t * length, oz(t) + (z / len) * rz(t) * round);
    }
    return finish(geometry, segments);
}

/** A muscle belly: a sphere stretched to sx/sy/sz whose width follows `width(t)` (t: 0 = bottom, 1 = top). */
function belly(sx, sy, sz, { width = constant(1), depth = width, bend = 0 } = {}) {
    const segments = 28;
    const geometry = new SphereGeometry(1, segments, 20);
    const pos = geometry.attributes.position;
    for (let i = 0; i < pos.count; i++) {
        const t = (pos.getY(i) + 1) / 2;
        const y = pos.getY(i) * sy;
        pos.setXYZ(i, pos.getX(i) * sx * width(t) + bend * y * y, y, pos.getZ(i) * sz * depth(t));
    }
    return finish(geometry, segments);
}

/** Normals, with the seam of the sphere smoothed so it doesn't show as a line. */
function finish(geometry, segments) {
    geometry.computeVertexNormals();
    const normal = geometry.attributes.normal;
    const row = segments + 1;
    for (let start = 0; start < normal.count; start += row) {
        const a = start;
        const b = start + segments;
        const nx = normal.getX(a) + normal.getX(b);
        const ny = normal.getY(a) + normal.getY(b);
        const nz = normal.getZ(a) + normal.getZ(b);
        const l = Math.hypot(nx, ny, nz) || 1;
        normal.setXYZ(a, nx / l, ny / l, nz / l);
        normal.setXYZ(b, nx / l, ny / l, nz / l);
    }
    return geometry;
}

/** Body proportions (half-widths, in figure units). */
const P = {
    hip: 0.39,
    waist: 0.355,
    chest: 0.53,
    lats: 0.56,
    shoulderX: 0.57,
    hipJoint: 0.195,
};

export function createBody3d(container, { onSelect } = {}) {
    const p = P;
    const accent = cssColor('--accent', '#c6ff3d');
    const idle = new Color('#9aa3b2');

    /* ---------- renderer, scene, camera ---------- */
    const renderer = new WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.outputColorSpace = SRGBColorSpace;
    renderer.toneMapping = ACESFilmicToneMapping;
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = PCFShadowMap;
    renderer.domElement.className = 'body3d-canvas';
    container.append(renderer.domElement);

    const scene = new Scene();
    const camera = new PerspectiveCamera(30, 1, 0.1, 100);
    camera.position.set(0, 2.12, 8.9);
    camera.lookAt(0, 2.06, 0);

    scene.add(new HemisphereLight(0xffffff, 0x1b1d22, 1.25));
    const key = new DirectionalLight(0xffffff, 2.4);
    key.position.set(2.5, 6, 5);
    key.castShadow = true;
    key.shadow.mapSize.set(1024, 1024);
    key.shadow.camera.left = -3;
    key.shadow.camera.right = 3;
    key.shadow.camera.top = 5;
    key.shadow.camera.bottom = -1;
    key.shadow.radius = 6;
    scene.add(key);
    const rim = new DirectionalLight(accent, 2.6); // gym-style colored back light
    rim.position.set(-4, 4, -5);
    scene.add(rim);
    const rim2 = new DirectionalLight(0x9fc1ff, 1.1);
    rim2.position.set(4.5, 3, -4);
    scene.add(rim2);
    const fill = new DirectionalLight(0xbfd4ff, 0.55);
    fill.position.set(-5, 2, 4);
    scene.add(fill);

    const floor = new Mesh(new CircleGeometry(1.6, 48), new ShadowMaterial({ opacity: 0.35 }));
    floor.rotation.x = -Math.PI / 2;
    floor.receiveShadow = true;
    scene.add(floor);

    /* ---------- materials ---------- */
    const figure = new Group();
    scene.add(figure);

    const skin = new MeshStandardMaterial({ color: new Color('#4b515c'), roughness: 0.55, metalness: 0.15 });
    const fabric = new MeshStandardMaterial({ color: new Color('#23262e'), roughness: 0.75, metalness: 0 });
    const muscleMaterials = Object.fromEntries(MUSCLE_GROUPS.map((group) => [group,
        new MeshStandardMaterial({ color: idle.clone(), roughness: 0.38, metalness: 0.12, emissive: accent, emissiveIntensity: 0 })]));

    const mesh = (geometry, material, parent = figure) => {
        const m = new Mesh(geometry, material);
        m.castShadow = true;
        parent.add(m);
        return m;
    };

    /** A muscle mesh, mirrored to the other side of the body when `mirror` is set. */
    const muscle = (group, geometry, [x, y, z], { rx = 0, ry = 0, rz = 0, parent = figure, mirror = true } = {}) => {
        const sides = mirror && x !== 0 ? [1, -1] : [1];
        for (const side of sides) {
            const m = mesh(geometry, muscleMaterials[group], parent);
            m.position.set(x * side, y, z);
            m.rotation.set(rx, ry * side, rz * side);
            m.scale.x = side; // mirror the shape itself too
            m.userData.muscle = group;
        }
    };

    /**
     * A limb segment from `from` to `to`: its local +y runs along the limb and local +z faces the front of the body,
     * so muscles can be placed with simple local coordinates (y = how far down the limb, z > 0 = front).
     */
    const segment = (from, to) => {
        const group = new Group();
        const start = new Vector3(...from);
        const axis = new Vector3(...to).sub(start);
        const length = axis.length();
        const y = axis.normalize();
        const z = FRONT.clone().addScaledVector(y, -FRONT.dot(y)).normalize();
        const x = new Vector3().crossVectors(y, z);
        group.position.copy(start);
        group.matrix.makeBasis(x, y, z);
        group.quaternion.setFromRotationMatrix(group.matrix);
        figure.add(group);
        // +1 when local +x points away from the body's center line (lateral), -1 otherwise
        const lateral = Math.sign(x.x * from[0]) || 1;
        return { group, length, lateral };
    };

    /* ---------- torso ---------- */
    const TORSO_BOTTOM = 1.76;
    const TORSO_TOP = 3.62;
    const torsoLength = TORSO_TOP - TORSO_BOTTOM;
    const tAt = (worldY) => (TORSO_TOP - worldY) / torsoLength; // lathe t: 0 = top
    const torsoRx = curve([[0, 0.13], [0.07, 0.3], [0.15, p.lats * 0.8], [0.24, p.lats], [0.37, p.chest],
        [0.52, (p.chest + p.waist) / 2 + 0.02], [0.63, p.waist], [0.76, p.hip], [0.88, p.hip - 0.02], [1, 0.3]]);
    const torsoRz = curve([[0, 0.11], [0.07, 0.17], [0.2, 0.235], [0.33, 0.25], [0.5, 0.215], [0.63, 0.19],
        [0.78, 0.215], [0.9, 0.2], [1, 0.16]]);
    const torsoOz = curve([[0, -0.01], [0.33, 0.015], [0.63, 0], [0.85, -0.015], [1, -0.01]]);
    // Shapes along the body axis are built as segments from top to bottom, so t = 0 is the top.
    const torso = segment([0, TORSO_TOP, 0], [0, TORSO_BOTTOM, 0]);
    mesh(lathe({ length: torsoLength, rx: torsoRx, rz: torsoRz, oz: torsoOz, segments: 64, rings: 64, cap: 0.3 }), skin, torso.group);

    /** Point on the torso surface at height y and sideways offset x (front: dir = 1, back: dir = -1). */
    const onTorso = (x, y, dir = 1, inset = 0) => {
        const t = tAt(y);
        const rx = torsoRx(t);
        const ratio = Math.min(0.98, Math.abs(x) / rx);
        return [x, y, torsoOz(t) + dir * (torsoRz(t) * Math.sqrt(1 - ratio * ratio) - inset)];
    };

    // shorts
    const shorts = lathe({
        length: 0.48,
        rx: curve([[0, p.hip + 0.012], [0.5, p.hip + 0.02], [1, p.hip - 0.03]]),
        rz: curve([[0, 0.222], [0.6, 0.225], [1, 0.2]]), cap: 0.06, segments: 48, rings: 24,
    });
    mesh(shorts, fabric, segment([0, 2.22, 0], [0, 1.74, 0]).group);

    // neck and head
    mesh(lathe({ length: 0.3, rx: curve([[0, 0.16], [0.45, 0.125], [1, 0.12]]), rz: curve([[0, 0.13], [1, 0.12]]), cap: 0.1 }), skin)
        .position.set(0, 3.5, -0.015);
    mesh(lathe({
        length: 0.52,
        rx: curve([[0, 0.03], [0.14, 0.15], [0.42, 0.18], [0.7, 0.165], [0.88, 0.12], [1, 0.04]]),
        rz: curve([[0, 0.03], [0.15, 0.18], [0.45, 0.215], [0.75, 0.19], [0.9, 0.13], [1, 0.04]]),
        oz: curve([[0, 0], [0.5, 0.01], [0.85, 0.04], [1, 0.04]]), cap: 0.25,
    }), skin, segment([0, 4.2, 0], [0, 3.68, 0]).group);

    /* ---------- torso muscles ---------- */
    // chest: two fan-shaped pecs, fuller at the bottom-outer edge
    muscle('CHEST', belly(0.245, 0.17, 0.1, { width: curve([[0, 0.85], [0.45, 1], [1, 0.8]]) }),
        onTorso(0.17, 3.11, 1, 0.05), { ry: 0.25, rz: -0.08, rx: -0.15 });
    // abs: four rows of pads plus obliques
    const abs = [[2.91, 0.09, 0.088], [2.74, 0.095, 0.088], [2.57, 0.093, 0.086], [2.4, 0.082, 0.08]];
    for (const [y, w, h] of abs) {
        muscle('CORE', belly(w, h, 0.045, { width: curve([[0, 0.9], [0.5, 1], [1, 0.92]]) }), onTorso(0.098, y, 1, 0.014));
    }
    muscle('CORE', belly(0.1, 0.27, 0.085, { width: curve([[0, 0.75], [0.6, 1], [1, 0.8]]) }),
        onTorso(0.265, 2.58, 1, 0.045), { ry: 0.6, rz: -0.1 });
    // serratus: three small "fingers" between the chest and the obliques
    for (const [y, x] of [[2.98, 0.36], [2.9, 0.355], [2.82, 0.345]]) {
        muscle('CORE', belly(0.045, 0.03, 0.04), onTorso(x, y, 1, 0.012), { ry: 0.9, rz: 0.35 });
    }
    // shoulders: a full deltoid cap, rounder at the front and side
    muscle('SHOULDERS', belly(0.165, 0.21, 0.18, { width: curve([[0, 0.45], [0.55, 1], [1, 0.95]]) }),
        [p.shoulderX + 0.02, 3.26, 0.0], { rz: 0.3 });
    // back: traps (diamond + slope to the neck), lats (wings), spinal erectors
    muscle('BACK', belly(0.3, 0.42, 0.075, { width: curve([[0, 0.05], [0.45, 0.8], [0.72, 1], [1, 0.5]]) }),
        onTorso(0, 3.15, -1, 0.03), { mirror: false });
    muscle('BACK', belly(0.19, 0.075, 0.1), [0.2, 3.46, -0.05], { rz: -0.42 });
    muscle('BACK', belly(0.22, 0.43, 0.095, { width: curve([[0, 0.25], [0.55, 0.85], [1, 1]]) }),
        onTorso(0.29, 2.88, -1, 0.045), { rz: 0.22, ry: -0.6 });
    muscle('BACK', belly(0.06, 0.3, 0.05), onTorso(0.075, 2.45, -1, 0.018));
    // glutes, over the shorts
    muscle('GLUTES', belly(0.18, 0.19, 0.13, { width: curve([[0, 0.8], [0.4, 1], [1, 0.85]]) }),
        [0.15, 1.97, -0.135]);

    /* ---------- arms (relaxed A-pose, elbows slightly bent) ---------- */
    for (const side of [1, -1]) {
        const shoulder = [side * p.shoulderX, 3.3, -0.01];
        const elbow = [side * (p.shoulderX + 0.17), 2.74, -0.04];
        const wrist = [side * (p.shoulderX + 0.25), 2.2, 0.06];

        const upper = segment(shoulder, elbow);
        mesh(lathe({
            length: upper.length,
            rx: curve([[0, 0.12], [0.35, 0.11], [0.8, 0.085], [1, 0.078]]),
            rz: curve([[0, 0.115], [0.4, 0.1], [0.85, 0.082], [1, 0.075]]),
        }), skin, upper.group);
        const ul = upper.length;
        muscle('BICEPS', belly(0.078, 0.2, 0.078, { width: curve([[0, 0.55], [0.45, 1], [1, 0.75]]) }),
            [0, ul * 0.55, 0.062], { parent: upper.group, mirror: false });
        muscle('TRICEPS', belly(0.062, 0.19, 0.062, { width: curve([[0, 0.5], [0.6, 1], [1, 0.8]]) }),
            [upper.lateral * 0.035, ul * 0.42, -0.06], { parent: upper.group, mirror: false });
        muscle('TRICEPS', belly(0.05, 0.16, 0.055), [-upper.lateral * 0.035, ul * 0.48, -0.062],
            { parent: upper.group, mirror: false });

        const fore = segment(elbow, wrist);
        mesh(lathe({
            length: fore.length,
            rx: curve([[0, 0.09], [0.25, 0.1], [0.75, 0.062], [1, 0.05]]),
            rz: curve([[0, 0.08], [0.25, 0.085], [0.75, 0.05], [1, 0.04]]),
        }), skin, fore.group);
        const hand = segment(wrist, [wrist[0] + side * 0.02, wrist[1] - 0.24, wrist[2] + 0.02]);
        mesh(lathe({
            length: hand.length,
            rx: curve([[0, 0.05], [0.35, 0.065], [0.75, 0.055], [1, 0.025]]),
            rz: curve([[0, 0.035], [0.4, 0.035], [1, 0.02]]), cap: 0.3,
        }), skin, hand.group);
    }

    /* ---------- legs ---------- */
    for (const side of [1, -1]) {
        const hip = [side * p.hipJoint, 1.92, 0];
        const knee = [side * 0.22, 0.99, 0.025];
        const ankle = [side * 0.235, 0.13, -0.02];

        const thigh = segment(hip, knee);
        const tl = thigh.length;
        mesh(lathe({
            length: tl,
            rx: curve([[0, 0.215], [0.25, 0.205], [0.7, 0.155], [1, 0.105]]),
            rz: curve([[0, 0.2], [0.3, 0.19], [0.75, 0.14], [1, 0.105]]),
        }), skin, thigh.group);
        // shorts leg
        mesh(lathe({ length: 0.3, rx: constant(0.222), rz: constant(0.208), cap: 0.05, segments: 36, rings: 16 }),
            fabric, thigh.group).position.y = -0.04;
        const lat = thigh.lateral;
        muscle('QUADS', belly(0.078, tl * 0.36, 0.075, { width: curve([[0, 0.55], [0.5, 1], [1, 0.7]]) }),
            [0, tl * 0.5, 0.13], { parent: thigh.group, mirror: false }); // rectus femoris
        muscle('QUADS', belly(0.085, tl * 0.36, 0.08, { width: curve([[0, 0.5], [0.5, 1], [1, 0.8]]) }),
            [lat * 0.11, tl * 0.5, 0.065], { parent: thigh.group, mirror: false, rz: lat * 0.05 }); // vastus lateralis
        muscle('QUADS', belly(0.075, tl * 0.15, 0.07, { width: curve([[0, 0.7], [0.4, 1], [1, 0.45]]) }),
            [-lat * 0.085, tl * 0.79, 0.075], { parent: thigh.group, mirror: false }); // vastus medialis (teardrop)
        for (const x of [0.065, -0.06]) {
            muscle('HAMSTRINGS', belly(0.07, tl * 0.34, 0.072, { width: curve([[0, 0.5], [0.5, 1], [1, 0.75]]) }),
                [lat * x, tl * 0.52, -0.115], { parent: thigh.group, mirror: false });
        }
        // knee cap
        mesh(belly(0.07, 0.075, 0.045), skin, thigh.group).position.set(0, tl * 0.98, 0.085);

        const shin = segment(knee, ankle);
        const sl = shin.length;
        mesh(lathe({
            length: sl,
            rx: curve([[0, 0.105], [0.25, 0.118], [0.65, 0.075], [1, 0.058]]),
            rz: curve([[0, 0.095], [0.28, 0.115], [0.65, 0.065], [1, 0.055]]),
            oz: curve([[0, 0], [0.28, -0.025], [0.7, 0], [1, 0]]),
        }), skin, shin.group);
        for (const x of [0.045, -0.04]) {
            muscle('CALVES', belly(0.07, sl * 0.2, 0.075, { width: curve([[0, 0.45], [0.55, 1], [1, 0.8]]) }),
                [shin.lateral * x, sl * 0.3, -0.095], { parent: shin.group, mirror: false });
        }
        // foot
        const foot = mesh(belly(0.085, 0.06, 0.18, { width: curve([[0, 1], [1, 0.85]]) }), skin);
        foot.position.set(side * 0.245, 0.06, 0.075);
    }

    figure.traverse((object) => { if (object.isMesh) object.castShadow = true; });

    /* ---------- state: heat colors, hover, selection ---------- */
    let heat = {};
    let hovered = null;
    let selected = null;

    const paint = () => {
        for (const group of MUSCLE_GROUPS) {
            const level = Math.min(1, (heat[group] ?? 0) / 12); // ~12 sets a week = fully lit
            const material = muscleMaterials[group];
            material.color.copy(idle).lerp(accent, level > 0 ? 0.35 + level * 0.65 : 0);
            material.emissiveIntensity = group === selected ? 0.55 : group === hovered ? 0.25 : level * 0.12;
        }
        requestRender();
    };

    /* ---------- interaction: drag to rotate, tap to select ---------- */
    const raycaster = new Raycaster();
    const pointer = new Vector2();
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    let autoRotate = !reduceMotion;
    let velocity = 0;
    let drag = null;

    const pick = (event) => {
        const rect = renderer.domElement.getBoundingClientRect();
        pointer.set(((event.clientX - rect.left) / rect.width) * 2 - 1, -((event.clientY - rect.top) / rect.height) * 2 + 1);
        raycaster.setFromCamera(pointer, camera);
        const hit = raycaster.intersectObjects(figure.children, true)[0];
        return hit?.object.userData.muscle ?? null;
    };

    const canvas = renderer.domElement;
    canvas.addEventListener('pointerdown', (event) => {
        drag = { x: event.clientX, startX: event.clientX, startY: event.clientY, time: performance.now() };
        autoRotate = false;
        canvas.setPointerCapture(event.pointerId);
    });
    canvas.addEventListener('pointermove', (event) => {
        if (drag) {
            const dx = event.clientX - drag.x;
            drag.x = event.clientX;
            figure.rotation.y += dx * 0.012;
            velocity = dx * 0.012;
            requestRender();
        } else if (event.pointerType === 'mouse') {
            const group = pick(event);
            if (group !== hovered) {
                hovered = group;
                canvas.style.cursor = group ? 'pointer' : 'grab';
                paint();
            }
        }
    });
    canvas.addEventListener('pointerup', (event) => {
        const moved = Math.hypot(event.clientX - drag.startX, event.clientY - drag.startY);
        drag = null;
        if (moved < 6) {
            velocity = 0;
            const group = pick(event);
            if (group) {
                selected = group;
                paint();
                onSelect?.(group);
            }
        }
    });
    canvas.addEventListener('pointercancel', () => { drag = null; });
    canvas.addEventListener('pointerleave', () => {
        if (hovered) {
            hovered = null;
            paint();
        }
    });

    /* ---------- render loop: only runs while visible and something moves ---------- */
    let frame = 0;
    let visible = true;
    let needsRender = true;

    function requestRender() {
        needsRender = true;
        if (!frame && visible) frame = requestAnimationFrame(loop);
    }

    function loop() {
        frame = 0;
        if (!canvas.isConnected) {
            dispose();
            return;
        }
        let moving = false;
        if (!drag && Math.abs(velocity) > 0.0005) {
            figure.rotation.y += velocity;
            velocity *= 0.93; // inertia after a swipe
            moving = true;
        } else if (autoRotate) {
            figure.rotation.y += 0.004;
            moving = true;
        }
        if (moving || needsRender) {
            renderer.render(scene, camera);
            needsRender = false;
        }
        if (moving && visible) frame = requestAnimationFrame(loop);
    }

    const resize = () => {
        const width = container.clientWidth || 300;
        const height = Math.round(width * 1.25);
        renderer.setSize(width, height, false);
        canvas.style.width = '100%';
        canvas.style.height = `${height}px`;
        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        requestRender();
    };
    const resizeObserver = new ResizeObserver(resize);
    resizeObserver.observe(container);
    const visibilityObserver = new IntersectionObserver(([entry]) => {
        visible = entry.isIntersecting;
        if (visible) requestRender();
    });
    visibilityObserver.observe(container);

    function dispose() {
        cancelAnimationFrame(frame);
        resizeObserver.disconnect();
        visibilityObserver.disconnect();
        renderer.dispose();
    }

    resize();
    paint();
    if (autoRotate) requestRender();

    return {
        setHeat(volume) {
            heat = volume ?? {};
            paint();
        },
        setSelected(group) {
            selected = group;
            paint();
        },
        /** Turns the figure to face front (0) or back (π). */
        face(side) {
            autoRotate = false;
            velocity = 0;
            const target = side === 'back' ? Math.PI : 0;
            const start = figure.rotation.y;
            const turns = Math.round((start - target) / (2 * Math.PI));
            const goal = target + turns * 2 * Math.PI;
            const began = performance.now();
            const step = (now) => {
                const k = Math.min(1, (now - began) / 500);
                figure.rotation.y = start + (goal - start) * (1 - (1 - k) ** 3);
                renderer.render(scene, camera);
                if (k < 1) requestAnimationFrame(step);
            };
            requestAnimationFrame(step);
        },
        dispose,
    };
}

/** True when the browser can draw WebGL (otherwise the flat SVG map is used). */
export function webglAvailable() {
    try {
        const canvas = document.createElement('canvas');
        return Boolean(window.WebGLRenderingContext && (canvas.getContext('webgl2') || canvas.getContext('webgl')));
    } catch {
        return false;
    }
}
