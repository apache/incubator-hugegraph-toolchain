/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
