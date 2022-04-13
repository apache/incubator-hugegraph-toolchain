import { useState, useEffect } from 'react';

export default function useMultiKeyPress() {
  const [keysPressed, setKeyPressed] = useState<Set<string>>(new Set());

  const keydownHandler = (e: KeyboardEvent) => {
    // use e.key here may cause some unexpected behavior with shift key
    setKeyPressed((prev) => new Set(prev.add(e.code)));
  };

  const keyupHandler = (e: KeyboardEvent) => {
    // use e.key here may cause some unexpected behavior with shift key
    if (navigator.platform.includes('Mac') && e.code.includes('Meta')) {
      // weired, like above we need to mutate keysPressed first
      keysPressed.clear();
      setKeyPressed(new Set());
      return;
    }

    keysPressed.delete(e.code);
    setKeyPressed(new Set(keysPressed));
  };

  useEffect(() => {
    window.addEventListener('keydown', keydownHandler);
    window.addEventListener('keyup', keyupHandler);

    return () => {
      window.removeEventListener('keydown', keydownHandler);
      window.removeEventListener('keyup', keyupHandler);
    };
  }, []);

  return keysPressed;
}
