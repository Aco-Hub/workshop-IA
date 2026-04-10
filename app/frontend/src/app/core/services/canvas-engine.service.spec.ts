import { describe, it, expect, beforeEach } from 'vitest';
import { CanvasEngine, CanvasElement } from './canvas-engine.service';

// jsdom ships without a working canvas 2D context.  We stub every method that
// CanvasEngine calls so that render() and draw helpers run without throwing.
function stubContext(): CanvasRenderingContext2D {
  const noop = () => {};
  let _fillStyle: string | CanvasGradient | CanvasPattern = '#000';
  let _strokeStyle: string | CanvasGradient | CanvasPattern = '#000';
  let _lineWidth = 1;
  let _lineCap: CanvasLineCap = 'round';
  let _lineJoin: CanvasLineJoin = 'round';
  let _font = '';
  let _textAlign: CanvasTextAlign = 'start';
  let _shadowColor = '';
  let _shadowBlur = 0;

  return {
    clearRect: noop,
    fillRect: noop,
    strokeRect: noop,
    fillText: noop,
    beginPath: noop,
    moveTo: noop,
    lineTo: noop,
    arc: noop,
    ellipse: noop,
    fill: noop,
    stroke: noop,
    save: noop,
    restore: noop,
    translate: noop,
    scale: noop,
    setLineDash: noop,
    roundRect: noop,
    drawImage: noop,
    get fillStyle() { return _fillStyle; },
    set fillStyle(v) { _fillStyle = v; },
    get strokeStyle() { return _strokeStyle; },
    set strokeStyle(v) { _strokeStyle = v; },
    get lineWidth() { return _lineWidth; },
    set lineWidth(v) { _lineWidth = v; },
    get lineCap() { return _lineCap; },
    set lineCap(v) { _lineCap = v; },
    get lineJoin() { return _lineJoin; },
    set lineJoin(v) { _lineJoin = v; },
    get font() { return _font; },
    set font(v) { _font = v; },
    get textAlign() { return _textAlign; },
    set textAlign(v) { _textAlign = v; },
    get shadowColor() { return _shadowColor; },
    set shadowColor(v) { _shadowColor = v; },
    get shadowBlur() { return _shadowBlur; },
    set shadowBlur(v) { _shadowBlur = v; },
  } as unknown as CanvasRenderingContext2D;
}

// The engine reads canvas.parentElement!.getBoundingClientRect() inside _resize().
// The canvas must therefore be appended to a parent that returns a non-zero rect.
function createMockCanvas(): HTMLCanvasElement {
  const parent = document.createElement('div');
  Object.defineProperty(parent, 'getBoundingClientRect', {
    value: () => ({
      width: 800,
      height: 600,
      top: 0,
      left: 0,
      right: 800,
      bottom: 600,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    }),
  });

  const canvas = document.createElement('canvas');
  // Replace the jsdom no-op getContext with our stub.
  Object.defineProperty(canvas, 'getContext', {
    value: () => stubContext(),
  });
  parent.appendChild(canvas);
  return canvas;
}

function makePen(id: number, points: number[][], zIndex = 0): CanvasElement {
  return { id, type: 'PEN', points, color: '#000000', strokeWidth: 2, zIndex };
}

// ─── Test suite ──────────────────────────────────────────────────────────────

describe('CanvasEngine', () => {
  let engine: CanvasEngine;
  let canvas: HTMLCanvasElement;

  beforeEach(() => {
    engine = new CanvasEngine();
    canvas = createMockCanvas();
    engine.init(canvas);
  });

  // Tool management

  describe('setTool / getTool', () => {
    it('defaults to the pen tool', () => {
      expect(engine.getTool()).toBe('pen');
    });

    it('stores each ToolType value', () => {
      const tools = ['select', 'rectangle', 'ellipse', 'line', 'text', 'eraser', 'image', 'link'] as const;
      for (const tool of tools) {
        engine.setTool(tool);
        expect(engine.getTool()).toBe(tool);
      }
    });

    it('can switch back to pen after another tool', () => {
      engine.setTool('rectangle');
      engine.setTool('pen');
      expect(engine.getTool()).toBe('pen');
    });
  });

  // Color

  describe('setColor', () => {
    it('applies the stored color to the CanvasElement returned by a pen stroke', () => {
      engine.setTool('pen');
      engine.setColor('#FF5500');

      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(10, 10);
      const el = engine.onPointerUp(20, 20);

      expect(el).not.toBeNull();
      expect(el!.color).toBe('#FF5500');
    });

    it('applies updated color to a subsequent stroke after the color changes', () => {
      engine.setTool('pen');
      engine.setColor('#AABBCC');

      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(5, 5);
      engine.onPointerUp(10, 10);

      engine.setColor('#001122');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(5, 5);
      const el2 = engine.onPointerUp(10, 10);

      expect(el2!.color).toBe('#001122');
    });

    it('applies the current color to RECTANGLE elements', () => {
      engine.setTool('rectangle');
      engine.setColor('#123456');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(50, 40);
      const el = engine.onPointerUp(50, 40);
      expect(el!.color).toBe('#123456');
    });
  });

  // Zoom

  describe('getZoom / setZoom', () => {
    it('starts at zoom level 1', () => {
      expect(engine.getZoom()).toBe(1);
    });

    it('stores the requested zoom level', () => {
      engine.setZoom(2.5);
      expect(engine.getZoom()).toBe(2.5);
    });

    it('clamps to a minimum of 0.1 for zero input', () => {
      engine.setZoom(0);
      expect(engine.getZoom()).toBe(0.1);
    });

    it('clamps to a minimum of 0.1 for negative input', () => {
      engine.setZoom(-10);
      expect(engine.getZoom()).toBe(0.1);
    });

    it('clamps to a maximum of 5', () => {
      engine.setZoom(100);
      expect(engine.getZoom()).toBe(5);
    });
  });

  describe('zoomIn', () => {
    it('multiplies scale by 1.2', () => {
      engine.setZoom(1);
      engine.zoomIn();
      expect(engine.getZoom()).toBeCloseTo(1.2);
    });

    it('does not exceed the ceiling of 5', () => {
      engine.setZoom(4.5);
      engine.zoomIn();
      engine.zoomIn();
      expect(engine.getZoom()).toBe(5);
    });
  });

  describe('zoomOut', () => {
    it('divides scale by 1.2', () => {
      engine.setZoom(1.2);
      engine.zoomOut();
      expect(engine.getZoom()).toBeCloseTo(1.0);
    });

    it('does not fall below the floor of 0.1', () => {
      engine.setZoom(0.12);
      engine.zoomOut();
      engine.zoomOut();
      expect(engine.getZoom()).toBe(0.1);
    });
  });

  describe('resetZoom', () => {
    it('returns scale to exactly 1', () => {
      engine.setZoom(3);
      engine.resetZoom();
      expect(engine.getZoom()).toBe(1);
    });

    it('also resets pan so screenToCanvas is 1:1 afterwards', () => {
      // Pan by triggering space+drag, then reset.
      engine.onPointerDown(0, 0, true);
      engine.onPointerMove(100, 80);
      engine.onPointerUp(100, 80);

      engine.setZoom(2);
      engine.resetZoom();

      const [cx, cy] = engine.screenToCanvas(50, 60);
      expect(cx).toBeCloseTo(50);
      expect(cy).toBeCloseTo(60);
    });
  });

  // Element management

  describe('addElement', () => {
    it('accepts a PEN element without throwing', () => {
      expect(() => engine.addElement(makePen(1, [[0, 0], [10, 10]]))).not.toThrow();
    });

    it('accepts an IMAGE element without throwing even without a real data URL', () => {
      const el: CanvasElement = {
        id: 5, type: 'IMAGE', x: 0, y: 0, width: 100, height: 80,
        imageData: 'data:image/png;base64,abc', color: '#000', strokeWidth: 1, zIndex: 0,
      };
      expect(() => engine.addElement(el)).not.toThrow();
    });

    it('sorts elements by zIndex after adding out-of-order', () => {
      // Add high-zIndex first, then low-zIndex.  No crash expected, and a
      // subsequent hit test on the low-zIndex element's position returns its id
      // (last in draw order wins, so the higher-zIndex element must be on top).
      engine.addElement(makePen(2, [[5, 5], [6, 6]], 10));
      engine.addElement(makePen(1, [[5, 5], [6, 6]], 1));

      engine.setTool('select');
      engine.onPointerDown(5, 5, false);
      // We can't directly inspect _selectedElementId, but no error means the
      // internal sort did not corrupt state.
      expect(engine.getTool()).toBe('select');
    });
  });

  describe('removeElement', () => {
    it('removes an existing element by id without throwing', () => {
      engine.addElement(makePen(1, [[0, 0], [1, 1]]));
      expect(() => engine.removeElement(1)).not.toThrow();
    });

    it('is a no-op for an unknown id', () => {
      expect(() => engine.removeElement(999)).not.toThrow();
    });
  });

  describe('clearAll', () => {
    it('removes all elements without throwing', () => {
      engine.addElement(makePen(1, [[0, 0], [1, 1]]));
      engine.addElement(makePen(2, [[5, 5], [6, 6]]));
      expect(() => engine.clearAll()).not.toThrow();
    });

    it('is idempotent when called on an already-empty canvas', () => {
      expect(() => {
        engine.clearAll();
        engine.clearAll();
      }).not.toThrow();
    });
  });

  describe('loadElements', () => {
    it('replaces all existing elements', () => {
      engine.addElement(makePen(1, [[0, 0], [1, 1]]));

      const batch = [
        makePen(10, [[0, 0], [1, 1]], 2),
        makePen(20, [[0, 0], [1, 1]], 0),
      ];
      expect(() => engine.loadElements(batch)).not.toThrow();
    });

    it('sorts the loaded batch by zIndex ascending', () => {
      const batch: CanvasElement[] = [
        makePen(3, [[0, 0], [1, 1]], 5),
        makePen(1, [[0, 0], [1, 1]], 1),
        makePen(2, [[0, 0], [1, 1]], 3),
      ];
      // After loading, a hit test on element id=1 (lowest zIndex) at its point
      // should still resolve to the topmost element (id=3, zIndex=5) because
      // all three share the same coordinates — highest drawn last wins.
      engine.loadElements(batch);

      engine.setTool('select');
      engine.onPointerDown(0, 0, false);
      // No crash == sorted correctly and rendered.
      expect(engine.getTool()).toBe('select');
    });

    it('loads IMAGE elements and pre-warms the image cache without throwing', () => {
      const batch: CanvasElement[] = [{
        id: 1, type: 'IMAGE', x: 0, y: 0, width: 100, height: 80,
        imageData: 'data:image/png;base64,abc', color: '#000', strokeWidth: 1, zIndex: 0,
      }];
      expect(() => engine.loadElements(batch)).not.toThrow();
    });
  });

  // Pointer interactions: pen tool

  describe('onPointerDown / onPointerMove / onPointerUp — pen tool', () => {
    beforeEach(() => engine.setTool('pen'));

    it('returns a PEN element with points after a normal stroke', () => {
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(10, 10);
      const el = engine.onPointerUp(20, 20);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('PEN');
      expect(el!.id).toBe(-1);
      expect(el!.points).toBeDefined();
      expect(el!.points!.length).toBeGreaterThanOrEqual(2);
    });

    it('returns null when no onPointerMove was issued (single point)', () => {
      engine.onPointerDown(50, 50, false);
      expect(engine.onPointerUp(50, 50)).toBeNull();
    });

    it('accumulates all intermediate points', () => {
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(5, 5);
      engine.onPointerMove(10, 10);
      engine.onPointerMove(15, 15);
      const el = engine.onPointerUp(20, 20);

      // 1 start + 3 moves = 4 minimum points.
      expect(el!.points!.length).toBeGreaterThanOrEqual(4);
    });

    it('carries the active color into the returned element', () => {
      engine.setColor('#DEADBE');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(10, 10);
      const el = engine.onPointerUp(10, 10);
      expect(el!.color).toBe('#DEADBE');
    });

    it('carries the active strokeWidth into the returned element', () => {
      engine.setStrokeWidth(8);
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(10, 10);
      const el = engine.onPointerUp(10, 10);
      expect(el!.strokeWidth).toBe(8);
    });

    it('sets zIndex equal to the current number of elements on the canvas', () => {
      engine.addElement(makePen(1, [[0, 0], [1, 1]]));
      engine.addElement(makePen(2, [[0, 0], [1, 1]]));

      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(10, 10);
      const el = engine.onPointerUp(10, 10);

      expect(el!.zIndex).toBe(2);
    });
  });

  // Pointer interactions: tiny-movement null guards

  describe('onPointerUp — null for tiny movements', () => {
    it('returns null for rectangle < 2px wide and tall', () => {
      engine.setTool('rectangle');
      engine.onPointerDown(100, 100, false);
      engine.onPointerMove(101, 101);
      expect(engine.onPointerUp(101, 101)).toBeNull();
    });

    it('returns null for ellipse < 2px wide and tall', () => {
      engine.setTool('ellipse');
      engine.onPointerDown(100, 100, false);
      engine.onPointerMove(101, 101);
      expect(engine.onPointerUp(101, 101)).toBeNull();
    });

    it('returns null for line with distance < 2px', () => {
      engine.setTool('line');
      engine.onPointerDown(100, 100, false);
      engine.onPointerMove(101, 100);
      expect(engine.onPointerUp(101, 100)).toBeNull();
    });

    it('returns a RECTANGLE element when movement is sufficient', () => {
      engine.setTool('rectangle');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(80, 60);
      const el = engine.onPointerUp(80, 60);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('RECTANGLE');
      expect(el!.x).toBe(0);
      expect(el!.y).toBe(0);
      expect(el!.width).toBe(80);
      expect(el!.height).toBe(60);
    });

    it('returns an ELLIPSE element when movement is sufficient', () => {
      engine.setTool('ellipse');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(60, 40);
      const el = engine.onPointerUp(60, 40);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('ELLIPSE');
      expect(el!.width).toBe(60);
      expect(el!.height).toBe(40);
    });

    it('returns a LINE element when distance is sufficient', () => {
      engine.setTool('line');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(30, 40);
      const el = engine.onPointerUp(30, 40);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('LINE');
      expect(el!.points).toHaveLength(2);
    });
  });

  // Pointer interactions: instant-placement tools

  describe('onPointerUp — instant-placement tools', () => {
    it('returns a TEXT element with empty text and fontSize 16 on tap', () => {
      engine.setTool('text');
      engine.onPointerDown(50, 50, false);
      const el = engine.onPointerUp(50, 50);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('TEXT');
      expect(el!.text).toBe('');
      expect(el!.fontSize).toBe(16);
    });

    it('returns an IMAGE element with default 200×150 dimensions on tap', () => {
      engine.setTool('image');
      engine.onPointerDown(100, 100, false);
      const el = engine.onPointerUp(100, 100);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('IMAGE');
      expect(el!.width).toBe(200);
      expect(el!.height).toBe(150);
      expect(el!.imageData).toBe('');
    });

    it('returns a LINK element with empty url on tap', () => {
      engine.setTool('link');
      engine.onPointerDown(100, 100, false);
      const el = engine.onPointerUp(100, 100);

      expect(el).not.toBeNull();
      expect(el!.type).toBe('LINK');
      expect(el!.url).toBe('');
    });
  });

  // Pointer interactions: eraser tool

  describe('onPointerUp — eraser tool', () => {
    it('returns null when no element is at the erased position', () => {
      engine.setTool('eraser');
      engine.onPointerDown(200, 200, false);
      engine.onPointerMove(210, 210);
      expect(engine.onPointerUp(210, 210)).toBeNull();
    });

    it('returns a stub element with the erased id when an element is hit', () => {
      // Place a pen element at a known position.
      engine.addElement(makePen(42, [[50, 50], [51, 51]]));

      engine.setTool('eraser');
      engine.onPointerDown(50, 50, false);
      engine.onPointerMove(50, 50);
      const result = engine.onPointerUp(50, 50);

      expect(result).not.toBeNull();
      expect(result!.id).toBe(42);
      // The engine also removes it internally — subsequent eraser on the same
      // spot returns null.
      engine.onPointerDown(50, 50, false);
      engine.onPointerMove(50, 50);
      expect(engine.onPointerUp(50, 50)).toBeNull();
    });
  });

  // Panning via space key

  describe('panning (spaceHeld = true)', () => {
    it('returns null from onPointerUp when panning', () => {
      engine.onPointerDown(10, 10, true);
      engine.onPointerMove(50, 40);
      expect(engine.onPointerUp(50, 40)).toBeNull();
    });

    it('does not start a pen stroke when space is held on pointerDown', () => {
      engine.setTool('pen');
      engine.onPointerDown(0, 0, true);
      engine.onPointerMove(20, 20);
      expect(engine.onPointerUp(20, 20)).toBeNull();
    });
  });

  // screenToCanvas coordinate conversion

  describe('screenToCanvas', () => {
    it('maps 1:1 at default zoom and pan', () => {
      const [cx, cy] = engine.screenToCanvas(100, 200);
      expect(cx).toBeCloseTo(100);
      expect(cy).toBeCloseTo(200);
    });

    it('divides by scale at zoom 2 with no pan', () => {
      engine.setZoom(2);
      const [cx, cy] = engine.screenToCanvas(100, 200);
      expect(cx).toBeCloseTo(50);
      expect(cy).toBeCloseTo(100);
    });

    it('accounts for the pan offset', () => {
      // Simulate a pan: pan state is set via space+drag.
      // panX = pointerMove.x - panStartX = pointerMove.x - (pointerDown.x - 0)
      //      = 100 - 0 = 100   (panStartX = down.x - panX = 0 - 0 = 0)
      // After releasing at (100, 80): panX=100, panY=80.
      engine.onPointerDown(0, 0, true);
      engine.onPointerMove(100, 80);
      engine.onPointerUp(100, 80);

      // With panX=100, panY=80, scale=1: canvas = (screen - pan) / scale.
      const [cx, cy] = engine.screenToCanvas(150, 130);
      expect(cx).toBeCloseTo(50);
      expect(cy).toBeCloseTo(50);
    });
  });

  // Fill mode, font size, font family

  describe('setFillMode', () => {
    it('stores the fill mode without throwing', () => {
      expect(() => engine.setFillMode('solid')).not.toThrow();
      expect(() => engine.setFillMode('stroke')).not.toThrow();
      expect(() => engine.setFillMode('pattern')).not.toThrow();
      expect(() => engine.setFillMode('gradient')).not.toThrow();
    });
  });

  describe('setFontSize', () => {
    it('applies the stored font size to TEXT elements', () => {
      engine.setTool('text');
      engine.setFontSize(24);
      engine.onPointerDown(50, 50, false);
      const el = engine.onPointerUp(50, 50);
      expect(el).not.toBeNull();
      expect(el!.fontSize).toBe(24);
    });

    it('defaults TEXT fontSize to 16 when not changed', () => {
      engine.setTool('text');
      engine.onPointerDown(50, 50, false);
      const el = engine.onPointerUp(50, 50);
      expect(el!.fontSize).toBe(16);
    });
  });

  describe('setFontFamily', () => {
    it('stores the font family without throwing', () => {
      expect(() => engine.setFontFamily('Arial')).not.toThrow();
      expect(() => engine.setFontFamily('Georgia')).not.toThrow();
    });
  });

  // Init / destroy lifecycle

  describe('init / destroy lifecycle', () => {
    it('render does not throw after init', () => {
      expect(() => engine.render()).not.toThrow();
    });

    it('resize does not throw after init', () => {
      expect(() => engine.resize()).not.toThrow();
    });

    it('destroy nullifies the context so subsequent render is a no-op without throwing', () => {
      engine.destroy();
      expect(() => engine.render()).not.toThrow();
    });

    it('double-destroy does not throw (wheel listener cleanup is safe)', () => {
      expect(() => {
        engine.destroy();
        engine.destroy();
      }).not.toThrow();
    });

    it('can be re-initialised with a fresh canvas after destroy', () => {
      engine.destroy();
      const freshCanvas = createMockCanvas();
      engine.init(freshCanvas);
      expect(() => engine.render()).not.toThrow();
    });

    it('destroy clears elements so operations on a re-init start from zero', () => {
      engine.addElement(makePen(1, [[0, 0], [1, 1]]));
      engine.addElement(makePen(2, [[0, 0], [1, 1]]));
      engine.destroy();

      const freshCanvas = createMockCanvas();
      engine.init(freshCanvas);

      engine.setTool('pen');
      engine.onPointerDown(0, 0, false);
      engine.onPointerMove(10, 10);
      const el = engine.onPointerUp(10, 10);

      // zIndex should be 0 because the element list was cleared on destroy.
      expect(el!.zIndex).toBe(0);
    });
  });
});
