import fs from 'node:fs';
import process from 'node:process';
import readlinePromises from 'node:readline';
import url from 'node:url';

import MathJax from "@mathjax/src";

// Font from command line.
const specifiedFont = process.argv[2];
let actualFont, extensionFont;

// Convert the fonts allowed by service into new format. Allowed font names listed here:
// https://docs.mathjax.org/en/v4.1/output/fonts.html
switch (specifiedFont) {
  case 'TeX':
    actualFont = 'mathjax-tex';
    break;
  case 'STIX-Web':
    actualFont = 'mathjax-stix2';
    break;
  case 'Asana-Math':
    actualFont = 'mathjax-asana';
    break;
  case 'Neo-Euler':
    actualFont = 'mathjax-tex';
    extensionFont = 'mathjax-euler';
    break;
  case 'Gyre-Pagella':
    actualFont = 'mathjax-pagella';
    break;
  case 'Gyre-Termes':
    actualFont = 'mathjax-termes';
    break;
  case 'Latin-Modern':
    actualFont = 'mathjax-modern';
    break;
  // Support all the mathjax names too.
  case 'mathjax-tex':
  case 'mathjax-stix2':
  case 'mathjax-asana':
  case 'mathjax-pagella':
  case 'mathjax-termes':
  case 'mathjax-modern':
  // This one is newly added so I used the same name.
  case 'mathjax-newcm':
    actualFont = specifiedFont;
    break;
}

// On Windows, we need to specifically use file URLs not paths or the ES6 loader doesn't work.
const baseUrl = url.pathToFileURL(import.meta.dirname);
const mjBundleUrl = baseUrl + '/node_modules/@mathjax/src/bundle';

// But then for some reason, we also need to use the full absolute path for SRE.
const dirName = import.meta.dirname.replaceAll('\\', '/');
const mjBundlePath = dirName + '/node_modules/@mathjax/src/bundle';

// Track errors in this array.
const mathjaxErrors = [];

const config = {
  loader: {
    paths: {
      mathjax: mjBundleUrl,
      // Override SRE path so it uses the path not file URL.
      'sre': mjBundlePath + '/sre',
    },
    load: [
      'input/tex',
      'input/mml',
      'output/svg',
      'ui/safe',
      'a11y/speech',
	  '[tex]/mhchem',
    ],
  },
  options: {
    enableSpeech: true,
    sre: {
      domain: 'mathspeak',
      style: 'default',
      locale: 'en',
    },
    a11y: {
      speech: true,
      braille: false,
    },
    compileError(document, math, error) {
      mathjaxErrors.push(error.message);
    },
  },
  svg: {
    blacker: 10,
    displayOverflow: 'overflow',
    linebreaks: {
      inline: false
    },
  },
  tex: {
    formatError(jax, error) {
      // Sometimes errors get reported twice, I don't know why but make sure it's only listed once.
      if (!mathjaxErrors.includes(error.message)) {
        mathjaxErrors.push(error.message);
      }
      return jax.formatError(error);
    }
  },
  output: {
    font: actualFont,
	displayAlign: 'left'
  },
};
if (extensionFont) {
  config.output.fontExtension = 'mathjax-euler';
}

// Load MathJax.
await MathJax.init(config);

const document = MathJax.startup.document;
const adaptor = MathJax.startup.adaptor;

async function processInput(input) {
  const options = {
    ex: 6,
    containerWidth: 100 * 6,
  };
  // Line breaks do not work the way I'd like in display math; they result in it
  // becoming the default width (100ex as above) rather than being sized to fit.
  // To resolve this, use inline math with displaystyle instead.
  let inputFormat = input.format;
  let inputValue = input.value;
  if (inputFormat === 'TeX') {
    inputFormat = 'inline-TeX';
    inputValue = '\\displaystyle{' + inputValue + '}';
  }
  let svg, mml;

  try {
    let svgContainer;
    if (inputFormat === 'MathML') {
      svgContainer = await MathJax.mathml2svgPromise(inputValue, options);
    } else {
      svgContainer = await MathJax.tex2svgPromise(inputValue, options);
      mml = await MathJax.tex2mmlPromise(inputValue, options);
    }

    // Find the speech text and put it as an SVG title.
    const speech = adaptor.getAttribute(svgContainer, 'data-semantic-speech-none');
    const svgElement = adaptor.getElement('svg', svgContainer);
    if (speech) {
      const title = adaptor.create('title', {}, [], 'http://www.w3.org/2000/svg');
      adaptor.insert(title, adaptor.firstChild(svgElement));
      adaptor.append(title, adaptor.text(speech));
    }

    // Remove all the data attributes, we don't need them and they make it way bigger.
    const removeDataAttributes = (root) => {
        const attributes = adaptor.allAttributes(root);
        for (const attribute of attributes) {
           if(attribute.name.startsWith('data-')) {
             adaptor.removeAttribute(root, attribute.name);
           }
        }
        const children = adaptor.childNodes(root);
        for (const child of children) {
          if(adaptor.kind(child) !== '#text') {
            removeDataAttributes(child);
          }
        }
    };
    removeDataAttributes(svgElement);

    svg = adaptor.serializeXML(svgElement);

	if (mml) {		
		// Bodge up the root level data-latex to the original value (without displaystyle).
		// Stick speech into the MathML as 'alttext' as well.
		const escapeXml = s => s
		  .replaceAll("&", "&amp;")
		  .replaceAll("<", "&lt;")
		  .replaceAll(">", "&gt;")
		  .replaceAll('"', "&quot;")
		  .replaceAll("'", "&apos;");

		const re = /(<math[^>]+)(>)/;
		mml = mml.replace(re, (match, g1, g2) => {
		  let fixedG1 = g1;
		  if (input.format === 'TeX') {
		    fixedG1 = fixedG1.replace(/ data-latex="[^"]*"/, ' data-latex="' + escapeXml(input.value.trim()) + '"');
		  }
		  return fixedG1 + (speech ? ' alttext="' + escapeXml(speech) + '"' : '') + g2;
		});
	}
  } catch(exception) {
    mathjaxErrors.push(exception.message);
  }

  process.stdout.write('<<BEGIN:RESULT\n');
  if (mathjaxErrors.length > 0) {
    process.stdout.write('<<BEGIN:ERRORS\n');
    // Output the errors and also clear the array ready for next equation.
    let maxErrors = 10;
    while (mathjaxErrors.length > 0) {
      process.stdout.write(mathjaxErrors.shift() + '\n');
      maxErrors--;
      if (maxErrors === 0) {
        process.stdout.write('Too many errors\n');
        break;
      }
    }
    process.stdout.write('<<END:ERRORS\n');
  } else {
    // Output SVG (which includes speech text and baseline info).
    process.stdout.write('<<BEGIN:SVG\n');
    process.stdout.write(svg);
    process.stdout.write('\n<<END:SVG\n');

    // Output MathML.
    if (input.format === 'TeX' || input.format === 'inline-TeX') {
      process.stdout.write('<<BEGIN:MATHML\n');
      process.stdout.write(mml);
      process.stdout.write('\n<<END:MATHML\n');
    }
  }
  process.stdout.write('<<END:RESULT\n');
}

// Prepare to read lines from stdin.
const rl = readlinePromises.createInterface({
  input: process.stdin,
  output: null,
  terminal: false
});

// Process input lines.
let mode = 'format';
let input = null;
rl.on('line', function(line) {
        process.stderr.write('Line read: [' + line + ']\n');

  switch (mode) {
    case 'format':
      if (line === 'TeX' || line === 'inline-TeX' || line === 'MathML') {
        input = { value: '', format: line };
        mode = 'input';
      } else {
        process.stderr.write('Invalid format: ' + line + '\n');
      }
      break;

    case 'input' :
      if (line === '') {
        processInput(input);
        mode = 'format';
      } else {
        input.value += line + '\n';
      }
      break;
  }
});
