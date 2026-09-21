import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';

export default function Login() {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await axios.post('http://localhost:8080/api/auth/login', formData);
      localStorage.setItem('token', response.data.token);
      alert('Login Successful!');
      navigate('/dashboard'); 
    } catch (err) {
      setError(err.response?.data || 'Invalid email or password');
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Left Pane - Clinic Image & Marketing */}
      <div className="hidden md:flex md:w-1/2 bg-slate-800 relative flex-col justify-between p-12 text-white overflow-hidden">
        <div className="absolute inset-0 bg-black/40 z-0"></div>
        <div className="relative z-10">
          <div className="flex items-center space-x-2 text-xl font-bold mb-16">
            <span className="text-blue-400">BeFit</span>
          </div>
          <h1 className="text-5xl font-bold leading-tight mb-6">
            Your Journey to Better Health Starts Here
          </h1>
          <p className="text-lg text-gray-200 mb-8 max-w-md">
            Advanced physiotherapy care, personalized treatment plans and expert guidance — for a healthier, stronger you.
          </p>
          <div className="flex flex-wrap gap-3">
            <span className="px-4 py-2 bg-white/20 rounded-full text-sm backdrop-blur-sm">Expert Therapists</span>
            <span className="px-4 py-2 bg-white/20 rounded-full text-sm backdrop-blur-sm">Multiple Branches</span>
            <span className="px-4 py-2 bg-white/20 rounded-full text-sm backdrop-blur-sm">Personalized Care</span>
          </div>
        </div>
      </div>

      {/* Right Pane - Login Form */}
      <div className="w-full md:w-1/2 flex flex-col justify-center px-8 md:px-24 bg-white">
        <div className="max-w-md w-full mx-auto">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Welcome Back!</h2>
          <p className="text-gray-500 mb-8">Log in to your patient account to continue</p>

          {error && <div className="mb-4 p-3 bg-red-100 text-red-700 rounded-md text-sm">{error}</div>}

          <form onSubmit={handleLogin} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="name@example.com"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                required
              />
            </div>

            <div>
              <div className="flex justify-between mb-1">
                <label className="block text-sm font-medium text-gray-700">Password</label>
                <a href="#" className="text-sm text-blue-600 hover:underline">Forgot password?</a>
              </div>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Enter your password"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                required
              />
            </div>

            <div className="flex items-center">
              <input type="checkbox" className="h-4 w-4 text-blue-600 border-gray-300 rounded" />
              <label className="ml-2 block text-sm text-gray-700">Remember me for 30 days</label>
            </div>

            <button
              type="submit"
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-lg transition-colors flex justify-center items-center"
            >
              Login &rarr;
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-gray-100">
            <div className="bg-blue-50 rounded-lg p-4 flex items-start space-x-3">
              <div className="bg-blue-100 p-2 rounded-full text-blue-600">🛡️</div>
              <div>
                <h4 className="text-sm font-semibold text-blue-900">Your data is secure with us</h4>
                <p className="text-xs text-blue-700 mt-1">We use industry-standard encryption protocols to ensure your personal health information remains private and protected at all times.</p>
              </div>
            </div>
          </div>
          
          <p className="text-center mt-6 text-sm text-gray-600">
            Don't have an account? <Link to="/register" className="text-blue-600 hover:underline font-medium">Register here</Link>
          </p>
        </div>
      </div>
    </div>
  );
}