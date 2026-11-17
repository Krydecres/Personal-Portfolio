import ReactMarkdown, { defaultUrlTransform } from 'react-markdown';
import remarkGfm from 'remark-gfm';

/**
 * Markdown renderer for posts and the bio.
 * - No rehype-raw: raw HTML inside Markdown is shown as text, never executed.
 * - defaultUrlTransform drops javascript:, vbscript:, data: (etc.) URLs from links and images.
 * - External links open with rel="noopener noreferrer" (no reverse tabnabbing, no Referer leak).
 */
const components = {
  a: ({ node: _node, href, children, ...rest }) => {
    const external = href && /^https?:\/\//i.test(href);
    return (
      <a href={href} {...rest} {...(external ? { target: '_blank', rel: 'noopener noreferrer nofollow' } : {})}>
        {children}
      </a>
    );
  },
  img: ({ node: _node, src, alt }) => (
    <img src={src} alt={alt || ''} loading="lazy" decoding="async" referrerPolicy="no-referrer" />
  ),
};

export default function Markdown({ children, className = 'prose' }) {
  return (
    <div className={className}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components} urlTransform={defaultUrlTransform}>
        {children || ''}
      </ReactMarkdown>
    </div>
  );
}
