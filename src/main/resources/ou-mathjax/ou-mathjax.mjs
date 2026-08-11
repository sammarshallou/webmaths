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
	  'input/mml/entities',
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
    // Note the 'blacker' option, which can be specified here, works by injecting style into
    // a CSS which is associated to the SVG but is not included within it, so it does nothing.
    // Super helpful.
    displayOverflow: 'overflow',
    linebreaks: {
      inline: false,
	  lineleading: .46,
    },
  },
  tex: {
    formatError(jax, error) {
      // We often get two copies of the same error from the SVG and MathML conversions.
      if (!mathjaxErrors.includes(error.message)) {
        mathjaxErrors.push(error.message);
      }
      return jax.formatError(error);
    }
  },
  output: {
    font: actualFont,
    displayAlign: 'left',
 },
};
if (extensionFont) {
  config.output.fontExtension = 'mathjax-euler';
}

// Load MathJax.
const mathjaxLoaded = await MathJax.init(config);
if (!mathjaxLoaded) {
    process.stderr.write('Fatal error: MathJax init failed\n');
    process.exit(1);
}

const document = MathJax.startup.document;
const adaptor = MathJax.startup.adaptor;

async function processInput(input) {
  const options = {
    ex: 6,
    containerWidth: 100 * 6,
  };
  // Line breaks do not work the way I'd like in inline math; they result in it
  // becoming specified as width 100% with no viewBox which breaks the Java
  // processing. To resolve this, use display math with displaystyle instead.
  let inputFormat = input.format;
  let inputValue = input.value;
  let fallbackInputValue = null;
  if (inputFormat === 'inline-TeX') {
	fallbackInputValue = inputValue;
    inputValue = '\\textstyle{' + inputValue + '}';
    inputFormat='TeX';
  }
  if (inputFormat === 'inline-TeX') {
    // This code never runs now, but I left it in just in case needed later.
    options.display = false;
  }
  let svg, mml;

  const ALREADY_GOT_ERROR = 'alreadyGotError';

  try {
    let svgContainer;

	// For inline equations, we have modified the TeX, so if it fails (which it does for example
	// if you do \begin{align*} inside the \textstyle) we are using a loop to bin it off and go
	// back to the original TeX. (It probably doesn't make sense to have these be inline, but it
	// should work since it did in older versions.)
	let attemptedPadding = false;
	while (true) {
		let innerException = null;
		try {
			// Clear error list.
			mathjaxErrors.length = 0;
			// Forget about any previous equation labels.
			MathJax.texReset();
			// Actually convert the equation using mathJax.
			if (inputFormat === 'MathML') {
			  svgContainer = await MathJax.mathml2svgPromise(inputValue, options);
			} else {
			  svgContainer = await MathJax.tex2svgPromise(inputValue, options);
			  // Forget about equation labels again, otherwise the same label will be used in
			  // both and it will fail.
			  MathJax.texReset();
			  mml = await MathJax.tex2mmlPromise(inputValue, options);
			}
			// If we succeed, break out of the loop.
			if (mathjaxErrors.length === 0) {
				// Check that the width is not 100%, if not then we're good to go.
				if (adaptor.getAttribute(adaptor.getElement('svg', svgContainer), 'width') != '100%') {
					break;
				}

				// For 100% width, try adding <mpadded> to mathml and \vtop to TeX.
				// This occurs when an equation has more than one line;
				// MathJax gives it width 100% so that it never appears inline and
				// potentially confuses the reader. See discussion on this issue:
				// https://github.com/mathjax/MathJax/issues/3607
				if (attemptedPadding) {
					// We already tried this and it didn't help.
					mathjaxErrors.push('Equation converted to SVG has unspecified width (even after trying to add padding)');
					break;
				}

				// Let's try again by adding a padding command.
				attemptedPadding = true;
				if (inputFormat === 'MathML') {
					// This regex works to insert <mpadded as the first child of <math...>...</math>
					// and also of <xyz:math...>...</xyz:math> in which case it inserts the prefix too.
					// Not sure anyone ever does that but just in case.
					inputValue = inputValue.replace(/^\s*(<([^>]+:)?math[^>]*>)(.*)(<\/([^>]+:)?math>)\s*/,
						'$1<$2mpadded>$3</$2mpadded>$4');
				} else {
					// For TeX we just add \vtop{...} around everything and cross our fingers.
					inputValue = '\\vtop{' + inputValue + '}';
				}

				// For some reason I don't understand (maybe it is because the <mpadded> makes this
				// count as one very tall line?), line leading tends to make these equations
				// look bad by adding extra space at the bottom, so set it back to default value.
				MathJax.startup.output.options.linebreaks.lineleading = 0.2;

				// Now retry the loop.
				continue;
			}
		} catch (exception) {
			innerException = exception;
		}

		// If we added padding and got an error, let's bail rather than being confusing giving
		// errors in equation code the user didn't actually send.
		if (attemptedPadding) {
			// Replace any errors with a general one.
			mathjaxErrors.length = 0;
			mathjaxErrors.push('Equation converted to SVG has unspecified width; attempt to modify equation failed');
			// This is a special code used later to put in the right error.
			throw new Error(ALREADY_GOT_ERROR);
		}

		// Exceptions that occur can be caused by us adding \textstyle, so let's try
		// the original value supplied by user.
		if (fallbackInputValue) {
			inputValue = fallbackInputValue;
			fallbackInputValue = null;
			// Now we continue around the loop.
		} else {
			// If there isn't an original value, just throw the exception.
			if (innerException) {
				throw exception;
			} else {
				// With no exception, we will just continue and return errors.
				break;
			}
		}
	}

    // Find the speech text and put it as an SVG title.
    const speech = adaptor.getAttribute(svgContainer, 'data-semantic-speech-none');
    const svgElement = adaptor.getElement('svg', svgContainer);
    if (speech) {
      const title = adaptor.create('title', {}, [], 'http://www.w3.org/2000/svg');
      adaptor.insert(title, adaptor.firstChild(svgElement));
      adaptor.append(title, adaptor.text(speech));
    }

    // Remove all the data attributes except data-c, we don't need them and they make
    // it way bigger.
    const removeDataAttributes = (root) => {
        const attributes = adaptor.allAttributes(root);
        for (const attribute of attributes) {
           if(attribute.name.startsWith('data-') && attribute.name != 'data-c') {
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

    // Insert CSS into the SVG so we can do the same thing as 'blacker'. This is based on MathJax
    // css which you can get by calling MathJax.svgStylesheet(), but that styling only works
    // within an mjx-container tag; this one is generic within the SVG.
    const styleText = 'path[data-c], use[data-c] { stroke-width: 5; }';
    const styleElement = adaptor.node('style', {}, [adaptor.text(styleText)]);
    adaptor.insert(styleElement, adaptor.firstChild(svgElement));

    svg = adaptor.serializeXML(svgElement);

    if (mml) {
        // Bodge up the root level data-latex to the original value (without textstyle).
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
	if (exception.message != ALREADY_GOT_ERROR) {
		// This could be an unexpected error, so log the full stack trace.
		mathjaxErrors.push(exception.stack);
	}
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
if (mathjaxLoaded) {
  const rl = readlinePromises.createInterface({
    input: process.stdin,
    output: null,
    terminal: false
  });

  // Process input lines.
  let mode = 'format';
  let input = null;
  let processingPromise = Promise.resolve();

  rl.on('line', function(line) {
    process.stderr.write('Line read: [' + line + ']\n');

    switch (mode) {
      case 'format':
        if (line === 'TeX' || line === 'inline-TeX' || line === 'MathML') {
          input = { value: '', format: line };
          mode = 'input';
        } else if (line ==='QUIT') {
          // Note this only works on the format line, so service callers can't make it exit by
          // writing QUIT into an equation.

		  // Wait for the last line to actually finish processing before we quit.
          processingPromise.then(() => {
              process.exit(0);
          });
      } else {
        process.stderr.write('Invalid format: ' + line + '\n');
      }
      break;

    case 'input' :
      if (line === '') {
        processingPromise = processInput(input);
        mode = 'format';
      } else {
        input.value += line + '\n';
      }
      break;
    }
  });
}
